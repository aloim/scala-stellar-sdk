package stellar.sdk

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import org.stellar.sdk.xdr.MemoType._
import org.stellar.sdk.xdr.{XdrDataInputStream, XdrDataOutputStream, Memo => XDRMemo}
import stellar.sdk.ByteArrays._

class MemoSpec extends Specification with ArbitraryInput with DomainMatchers {

  "memo none" should {
    "serialise to xdr" >> {
      NoMemo.toXDR.getDiscriminant mustEqual MEMO_NONE
    }
  }

  "a text memo" should {
    "not be constructable with > 28 bytes" >> prop { s: String =>
      MemoText(s) must throwAn[AssertionError]
    }.setGen(arbString.arbitrary.suchThat(_.getBytes("UTF-8").length > 28))

    "serialise to xdr" >> prop { s: String =>
      val text = s.foldLeft("") { case (acc, next) =>
        val append = s"$acc$next"
        if (append.getBytes("UTF-8").length < 28) append else acc
      }.mkString
      val memo = MemoText(text)
      val xdr = memo.toXDR
      xdr.getDiscriminant mustEqual MEMO_TEXT
      xdr.getText mustEqual text
    }
  }

  "a id memo" should {
    "not be constructable with zero" >> {
      MemoId(0) must throwAn[AssertionError]
    }

    "not be constructable with negative number" >> prop { id: Long =>
      MemoId(id) must throwAn[AssertionError]
    }.setGen(Gen.negNum[Long])

    "serialise to xdr" >> prop { id: Long =>
      val memo = MemoId(id)
      val xdr = memo.toXDR
      xdr.getDiscriminant mustEqual MEMO_ID
      xdr.getId.getUint64 mustEqual id
    }.setGen(Gen.posNum[Long])
  }

  "a memo hash" should {
    "not be constructable with > 32 bytes" >> {
      MemoHash((1 to 33).map(_.toByte).toArray) must throwAn[AssertionError]
    }

    "pad the input bytes" >> prop { bs: Array[Byte] =>
      MemoHash(bs.take(32)).bytes mustEqual paddedByteArray(bs.take(32), 32)
    }

    "provide padded hex value" >> prop { s: String =>
      val hex = MemoHash(s.take(32).getBytes("UTF-8")).hex
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8").trim mustEqual s.take(32)
      hex.length mustEqual 64
    }.setGen(Gen.identifier)

    "provide trimmed hex value" >> prop { s: String =>
      val hex = MemoHash(s.take(32).getBytes("UTF-8")).hexTrim
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8") mustEqual s.take(32)
    }.setGen(Gen.identifier)

    "serialise to xdr" >> prop { bs: Array[Byte] =>
      val memo = MemoHash(bs.take(32))
      val xdr = memo.toXDR
      xdr.getDiscriminant mustEqual MEMO_HASH
      xdr.getHash.getHash.toSeq mustEqual paddedByteArray(bs.take(32), 32).toSeq
    }

    "be created from a hash" >> prop { bs: Array[Byte] =>
      val hex = bs.take(32).map("%02X".format(_)).mkString
      MemoHash.from(hex) must beSuccessfulTry.like { case m: MemoHash =>
        m.bs.toSeq mustEqual bs.take(32).toSeq
      }
      MemoHash.from(s"${hex}Z") must beFailedTry[MemoHash]
      (MemoHash.from(bs.map("%02X".format(_)).mkString) must beFailedTry[MemoHash]).unless(bs.length <= 32)
    }
  }

  "a memo return hash" should {
    "not be constructable with > 32 bytes" >> {
      MemoReturnHash((1 to 33).map(_.toByte).toArray) must throwAn[AssertionError]
    }

    "pad the input bytes" >> prop { bs: Array[Byte] =>
      MemoReturnHash(bs.take(32)).bytes mustEqual paddedByteArray(bs.take(32), 32)
    }

    "provide padded hex value" >> prop { s: String =>
      val hex = MemoReturnHash(s.take(32).getBytes("UTF-8")).hex
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8").trim mustEqual s.take(32)
      hex.length mustEqual 64
    }.setGen(Gen.identifier)

    "provide trimmed hex value" >> prop { s: String =>
      val hex = MemoReturnHash(s.take(32).getBytes("UTF-8")).hexTrim
      new String(hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte), "UTF-8") mustEqual s.take(32)
    }.setGen(Gen.identifier)

    "serialise to xdr" >> prop { bs: Array[Byte] =>
      val memo = MemoReturnHash(bs.take(32))
      val xdr = memo.toXDR
      xdr.getDiscriminant mustEqual MEMO_RETURN
      xdr.getRetHash.getHash.toSeq mustEqual paddedByteArray(bs.take(32), 32).toSeq
    }

    "be created from a hash" >> prop { bs: Array[Byte] =>
      val hex = bs.take(32).map("%02X".format(_)).mkString
      MemoReturnHash.from(hex) must beSuccessfulTry.like { case m: MemoReturnHash =>
        m.bs.toSeq mustEqual bs.take(32).toSeq
      }
      MemoReturnHash.from(s"${hex}Z") must beFailedTry[MemoReturnHash]
      (MemoReturnHash.from(bs.map("%02X".format(_)).mkString) must beFailedTry[MemoReturnHash]).unless(bs.length <= 32)
    }
  }

  "every kind of memo" should {
    "be en/decoded to xdr stream" >> prop { memo: Memo =>
      memo must beEncodable
    }
  }

  private def beEncodable: Matcher[Memo] = { memo: Memo =>
    val xdrMemo = memo.toXDR
    val baos = new ByteArrayOutputStream()
    val os = new XdrDataOutputStream(baos)
    XDRMemo.encode(os, xdrMemo)
    val is = new XdrDataInputStream(new ByteArrayInputStream(baos.toByteArray))
    XDRMemo.decode(is) must beEquivalentTo(xdrMemo)
  }

}
