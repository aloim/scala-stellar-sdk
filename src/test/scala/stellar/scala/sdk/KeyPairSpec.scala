package stellar.scala.sdk

import org.apache.commons.codec.binary.Hex
import org.specs2.mutable.Specification

class KeyPairSpec extends Specification with ArbitraryInput {

  private val keyPair = KeyPair.fromSecretSeed(
    Hex.decodeHex("1123740522f11bfef6b3671f51e159ccf589ccf8965262dd5f97d1721d383dd4")
  )
  private val sig = "587d4b472eeef7d07aafcd0b049640b0bb3f39784118c2e2b73a04fa2f64c9c538b4b2d0f5335e968a480021fdc23e98c0ddf424cb15d8131df8cb6c4bb58309"

  "signed data" should {
    "be verified by the signing key" >> prop { msg: String =>
      keyPair.verify(msg.getBytes, keyPair.sign(msg.getBytes)) must beTrue
    }

    "be correct for concrete example" >> {
      val data = "hello world"
      val actual = keyPair.sign(data.getBytes)
      Hex.encodeHex(actual).mkString mustEqual sig
    }

    "verify true for a concrete example of a valid signature" >> {
      val data = "hello world"
      keyPair.verify(data.getBytes, Hex.decodeHex(sig)) must beTrue
    }

    "verify false for a concrete example of an invalid signature" >> {
      val data = "今日は世界"
      keyPair.verify(data.getBytes, Hex.decodeHex(sig)) must beFalse
    }
  }

  "a key pair" should {
    "report its account id and secret seed" >> prop { kp: KeyPair =>
      kp.accountId.toCharArray must haveLength(56)
      kp.accountId must startWith("G")
      KeyPair.fromPublicKey(kp.publicKey) must beLike { case VerifyingKey(pk) =>
        Hex.encodeHex(pk.getAbyte) mustEqual Hex.encodeHex(kp.pk.getAbyte)
      }
      KeyPair.fromSecretSeed(kp.secretSeed) must beLike { case KeyPair(pk, sk) =>
        Hex.encodeHex(pk.getAbyte) mustEqual Hex.encodeHex(kp.pk.getAbyte)
        Hex.encodeHex(sk.getAbyte) mustEqual Hex.encodeHex(kp.sk.getAbyte)
      }
    }
  }

}
