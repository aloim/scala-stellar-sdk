package stellar.sdk

import java.time.ZonedDateTime

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import stellar.sdk.SessionTestAccount.{accWithData, accn}
import stellar.sdk.inet.TxnFailure
import stellar.sdk.op.{CreateAccountOperation, PaymentOperation, Transacted}
import stellar.sdk.resp._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SequentialIntegrationSpec(implicit ee: ExecutionEnv) extends Specification with DomainMatchersIT {

  sequential

  "account endpoint" >> {
    "fetch account details" >> {
      TestNetwork.account(accn) must beLike[AccountResp] {
        case AccountResp(id, _, _, _, _, _, List(lumens), _) =>
          id mustEqual accn.accountId
          lumens mustEqual Amount.lumens(10000)
      }.awaitFor(30.seconds)
    }

    "fetch nothing if no account exists" >> {
      TestNetwork.account(KeyPair.random) must throwA[TxnFailure].awaitFor(5.seconds)
    }

    "return the data for an account" >> {
      TestNetwork.accountData(accWithData, "life_universe_everything") must beEqualTo("42").awaitFor(5.seconds)
    }

    "fetch nothing if no data exists for the account" >> {
      TestNetwork.accountData(accWithData, "brain_size_of_planet") must throwA[TxnFailure].awaitFor(5.seconds)
    }
  }

  "asset endpoint" should {
    "list all assets" >> {
      val oneFifteen = TestNetwork.assets().map(_.take(115))
      oneFifteen.map(_.distinct.size) must beEqualTo(115).awaitFor(10 seconds)
    }

    "filter assets by code" >> {
      val byCode = TestNetwork.assets(code = Some("ALX1")).map(_.take(10).toList)
      byCode.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byCode.map(_.map(_.asset.code).toSet) must beEqualTo(Set("ALX1")).awaitFor(10 seconds)
    }

    "filter assets by issuer" >> {
      val issuerAccount = "GCZAKXMQZKYJBQK7U2LFIF77PKGDCZRU3IOPV2VON5CHWJSWDH2B5A42"
      val byIssuer = TestNetwork.assets(issuer = Some(issuerAccount)).map(_.take(10).toList)
      byIssuer.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byIssuer.map(_.map(_.asset.issuer.accountId).toSet) must beEqualTo(Set(issuerAccount)).awaitFor(10 seconds)
    }

    "filter assets by code and issuer" >> {
      val issuerAccount = "GCZAKXMQZKYJBQK7U2LFIF77PKGDCZRU3IOPV2VON5CHWJSWDH2B5A42"
      val byCodeAndIssuer = TestNetwork.assets(code = Some("ALX1"), issuer = Some(issuerAccount)).map(_.toList)
      byCodeAndIssuer.map(_.map(_.asset)) must beLike[Seq[NonNativeAsset]] {
        case Seq(asset) => asset must beEquivalentTo(IssuedAsset4("ALX1", KeyPair.fromAccountId(issuerAccount)))
      }.awaitFor(10 seconds)
    }
  }

  "effect endpoint" should {
    "list all effects" >> {
      val oneFifteen = TestNetwork.effects().map(_.take(115))
      oneFifteen.map(_.distinct.size) must beEqualTo(115).awaitFor(10.seconds)
    }

    "filter effects by account" >> {
      val byAccount = TestNetwork.effectsByAccount(accn).map(_.take(10).toList)
      byAccount.map(_.isEmpty) must beFalse.awaitFor(10 seconds)
      byAccount.map(_.head) must beLike[EffectResp] {
        case EffectAccountCreated(_, account, startingBalance) =>
          account.accountId mustEqual accn.accountId
          startingBalance mustEqual Amount.lumens(10000)
      }.awaitFor(10.seconds)
    }

    "filter effects by ledger" >> {
      val byLedger = PublicNetwork.effectsByLedger(16237465).map(_.toList)
      byLedger must beEqualTo(Seq(
        EffectTrade("0069739381144948737-0000000001", 747605, KeyPair.fromAccountId("GD3IYBNQ45LXHFABSX4HLGDL7BQA62SVB5NB5O6XMBCITFZOLWLVS22B"),
          Amount(5484522, IssuedAsset4("XLM", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG"))),
          KeyPair.fromAccountId("GBBMSYSNV7PC6XAI3JL6F5OWP54TIONGDDTEJ4AQS3YMFUSPDSSSDQVB"),
          Amount(2445, IssuedAsset4("ETH", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG")))),

        EffectTrade("0069739381144948737-0000000002", 747605, KeyPair.fromAccountId("GBBMSYSNV7PC6XAI3JL6F5OWP54TIONGDDTEJ4AQS3YMFUSPDSSSDQVB"),
          Amount(2445, IssuedAsset4("ETH", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG"))),
          KeyPair.fromAccountId("GD3IYBNQ45LXHFABSX4HLGDL7BQA62SVB5NB5O6XMBCITFZOLWLVS22B"),
          Amount(5484522, IssuedAsset4("XLM", KeyPair.fromAccountId("GBSTRH4QOTWNSVA6E4HFERETX4ZLSR3CIUBLK7AXYII277PFJC4BBYOG")))
        )
      )).awaitFor(10.seconds)
    }
  }

  "ledger endpoint" should {
    "list the details of a given ledger" >> {
      PublicNetwork.ledger(16237465) must beEqualTo(
        LedgerResp("d4a8dae64397e23551a5b57e30ae16d6887b6a49fb9263808878fd6dc71f64be",
          "d4a8dae64397e23551a5b57e30ae16d6887b6a49fb9263808878fd6dc71f64be",
          Some("ec7d2a4c064a1f10741b93260fc5b625febdf8cc5a06df8a892396ecab4449d0"), 16237465L, 1, 1,
          ZonedDateTime.parse("2018-02-13T00:33:53Z"), 1.0368912397155042E11, 1415204.6354335, 100, 5000000, 50)
      ).awaitFor(10.seconds)
    }

    "list all ledgers" >> {
      val oneFifteen = TestNetwork.ledgers().map(_.take(115))
      oneFifteen.map(_.distinct.size) must beEqualTo(115).awaitFor(10.seconds)
    }
  }

  "offer endpoint" should {
    "list offers by account" >> {
      TestNetwork.offersByAccount(KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"))
        .map(_.toSeq) must beEqualTo(Seq(
        OfferResp(
          id = 101542,
          seller = KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"),
          selling = Amount.lumens(165),
          buying = IssuedAsset12(
            code = "sausage",
            issuer = KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK")),
          price = Price(303, 100)
        )
      )).awaitFor(10.seconds)
    }
  }

  "operation endpoint" should {
    "list all operations" >> {
      val oneThirty = TestNetwork.operations().map(_.take(130))
      oneThirty.map(_.distinct.size) must beEqualTo(130).awaitFor(10.seconds)
    }
    "list operations by account" >> {
      TestNetwork.operationsByAccount(KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"))
        .map(_.take(4).last) must beEqualTo(Transacted(
        id = 30985448851509249L,
        txnHash = "685bcbdf699139f3e244fa9af09c8108b55fd3c554c38f7d54a5c4a4ad1e38d4",
        sourceAccount = KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK"),
        createdAt = ZonedDateTime.parse("2018-02-08T10:18:39Z"),
        operation = PaymentOperation(
          destinationAccount = KeyPair.fromAccountId("GAR2WMVXCTFUXHU4K5KZNRAVTYFAFWT4XWFLKJ5IKEQ65Q47WNSMDVKH"),
          amount = IssuedAmount(10000000000L,
            IssuedAsset12("sausage", KeyPair.fromAccountId("GCXYKQF35XWATRB6AWDDV2Y322IFU2ACYYN5M2YB44IBWAIITQ4RYPXK")))
        ))).awaitFor(10.seconds)
    }
    val kinPayment = Transacted(
      id = 70009259709968385L,
      txnHash = "233ce5d17477706e097f72ae1c46241f4586ad1476d191119d46a93e88b9d3fa",
      sourceAccount = KeyPair.fromAccountId("GDBWXSZDYO4C3EHYXRLCGU3NP55LUBEQO5K2RWIWWMXWVI57L7VUWSZA"),
      createdAt = ZonedDateTime.parse("2018-02-16T09:37:30Z"),
      operation = PaymentOperation(
        destinationAccount = KeyPair.fromAccountId("GCT4TTKW2HPCMHM6PJHQ33FIIDCVKIJXLXDHMKQEC7DKHPPGLUKCHKY7"),
        amount = IssuedAmount(28553980000000L,
          IssuedAsset4("KIN", KeyPair.fromAccountId("GBDEVU63Y6NTHJQQZIKVTC23NWLQVP3WJ2RI2OTSJTNYOIGICST6DUXR")))
      )
    )
    "list operations by ledger" >> {
      PublicNetwork.operationsByLedger(16300301).map(_.last) must beEqualTo(kinPayment).awaitFor(10.seconds)
    }
    "list operations by transaction" >> {
      PublicNetwork.operationsByTransaction("233ce5d17477706e097f72ae1c46241f4586ad1476d191119d46a93e88b9d3fa")
        .map(_.head) must beEqualTo(kinPayment).awaitFor(10.seconds)
    }
    "list the details of a given operation" >> {
      PublicNetwork.operation(70009259709968385L) must beEqualTo(kinPayment).awaitFor(10.seconds)
    }
  }

  "orderbook endpoint" should {
    "fetch current orders" >> {
      // todo - replace with a static test network assertion
      val mobi = IssuedAsset4("MOBI", KeyPair.fromAccountId("GA6HCMBLTZS5VYYBCATRBRZ3BZJMAFUDKYYF6AH6MVCMGWMRDNSWJPIH"))
      Await.result(PublicNetwork.orderBook(
        selling = NativeAsset,
        buying = mobi
      ), 10.seconds) must beLike { case ob: OrderBook =>
        ob.selling mustEqual NativeAsset
        ob.buying mustEqual mobi
      }
    }
  }

  "payments endpoint" should {
    "fetch payments in pages" >> {
      TestNetwork.payments().map(_.take(130).last) must beEqualTo(
        Transacted(
          id = 91946659876865L,
          txnHash = "dd667058cb84fef012a102e5c6be22c532534f0182076d7eabced3e606b22d7d",
          sourceAccount = KeyPair.fromAccountId("GBK4EP3WICCDJQ3MSYUNRV3PNVQQZAESFGV4DALFENUFAGOY4J7QQNGW"),
          createdAt = ZonedDateTime.parse("2017-03-21T16:06:42Z"),
          operation = PaymentOperation(
            destinationAccount = KeyPair.fromAccountId("GD7E76FQQDNM5GXQX3SMEN5FIZKVHD2KYRYN2UPDQIDKHSXV4QUN7ZT3"),
            amount = IssuedAmount(
              units = 10000L,
              asset = IssuedAsset4(
                "USD",
                KeyPair.fromAccountId("GBK4EP3WICCDJQ3MSYUNRV3PNVQQZAESFGV4DALFENUFAGOY4J7QQNGW")
              )),
            sourceAccount = None))
      ).awaitFor(10.seconds)
    }

    "filter payments by account" >> {
      PublicNetwork.paymentsByAccount(KeyPair.fromAccountId("GDKUP3J2MXYLSMU556XDSPLGPH5NFITGRT3HSNGNCZP3HTYBZ6AVNB7N"))
        .map(_.drop(9).head) must beEqualTo(
        Transacted(
          id = 68867133416685569L,
          txnHash = "1459b596c081eb87829c9168e9eb044eebc434fd92e4b8bc59a195dbf5c4c123",
          sourceAccount = KeyPair.fromAccountId("GDKUP3J2MXYLSMU556XDSPLGPH5NFITGRT3HSNGNCZP3HTYBZ6AVNB7N"),
          createdAt = ZonedDateTime.parse("2018-02-02T08:55:47Z"),
          operation = PaymentOperation(
            destinationAccount = KeyPair.fromAccountId("GAHK7EEG2WWHVKDNT4CEQFZGKF2LGDSW2IVM4S5DP42RBW3K6BTODB4A"),
            amount = Amount.lumens(6440),
            sourceAccount = None))
      ).awaitFor(10.seconds)
    }

    "filter payments by ledger" >> {
      PublicNetwork.paymentsByLedger(16034375).map(_.head) must beEqualTo(
        Transacted(
          id = 68867116236808193L,
          txnHash = "4d0c7f118ca939fe10ba3e1facc601814e3e0135991634fe43f019450fa2b5cd",
          sourceAccount = KeyPair.fromAccountId("GA5XIGA5C7QTPTWXQHY6MCJRMTRZDOSHR6EFIBNDQTCQHG262N4GGKTM"),
          createdAt = ZonedDateTime.parse("2018-02-02T08:55:32Z"),
          operation = PaymentOperation(
            destinationAccount = KeyPair.fromAccountId("GAHK7EEG2WWHVKDNT4CEQFZGKF2LGDSW2IVM4S5DP42RBW3K6BTODB4A"),
            amount = Amount.lumens(19999.99998),
            sourceAccount = None))
      ).awaitFor(10.seconds)
    }

    "filter payments by transaction" >> {
      PublicNetwork.paymentsByTransaction("1459b596c081eb87829c9168e9eb044eebc434fd92e4b8bc59a195dbf5c4c123")
        .map(_.head) must beEqualTo(
        Transacted(
          id = 68867133416685569L,
          txnHash = "1459b596c081eb87829c9168e9eb044eebc434fd92e4b8bc59a195dbf5c4c123",
          sourceAccount = KeyPair.fromAccountId("GDKUP3J2MXYLSMU556XDSPLGPH5NFITGRT3HSNGNCZP3HTYBZ6AVNB7N"),
          createdAt = ZonedDateTime.parse("2018-02-02T08:55:47Z"),
          operation = PaymentOperation(
            destinationAccount = KeyPair.fromAccountId("GAHK7EEG2WWHVKDNT4CEQFZGKF2LGDSW2IVM4S5DP42RBW3K6BTODB4A"),
            amount = Amount.lumens(6440),
            sourceAccount = None))
      ).awaitFor(10.seconds)
    }
  }

  "trades endpoint" should {
    "fetch trades in pages" >> {
      PublicNetwork.trades().map(_.take(230).last) must beEqualTo(
        Trade(
          id = "39969884779581441-0",
          ledgerCloseTime = ZonedDateTime.parse("2017-02-25T22:51:46Z"),
          offerId = 2585L,
          baseAccount = KeyPair.fromAccountId("GA6KWMT33N63HIV2NZ3AFWENTYI4ZJSNAXQMTQI3XJK5GORN36SETSNF"),
          baseAmount = IssuedAmount(
            units = 200000000,
            asset = IssuedAsset4("PHP", KeyPair.fromAccountId("GBUQWP3BOUZX34TOND2QV7QQ7K7VJTG6VSE7WMLBTMDJLLAW7YKGU6EP"))
          ),
          counterAccount = KeyPair.fromAccountId("GBZ2KAUWAKA7UKYUHTTXQ3QKXG3HTMMP33BLYJIHUTIXSXMOR6OD2ITZ"),
          counterAmount = IssuedAmount(
            units = 4000000,
            asset = IssuedAsset4("USD", KeyPair.fromAccountId("GDLL2FS5TXV5PCXFYJSLRZFCYEV52QY726R4XYBZKICKQLRARR5Q4SSK"))
          ),
          baseIsSeller = true)
      ).awaitFor(10.seconds)
    }

    "filter trades by orderbook" >> {
      PublicNetwork.tradesByOrderBook(
        base = NativeAsset,
        counter = IssuedAsset4("SLT", KeyPair.fromAccountId("GCKA6K5PCQ6PNF5RQBF7PQDJWRHO6UOGFMRLK3DYHDOI244V47XKQ4GP"))
      ).map(_.take(10).last) must beEqualTo(
        Trade(
          id = "61564881559621633-0",
          ledgerCloseTime = ZonedDateTime.parse("2017-11-02T15:34:05Z"),
          offerId = 187430L,
          baseAccount = KeyPair.fromAccountId("GDLHSCWRFUNEEJL6PR67OZL7QVO2L57MKQOMS6LGKNLGZPX6KCHXREMP"),
          baseAmount = Amount.lumens(0.1457620),
          counterAccount = KeyPair.fromAccountId("GAYDG77BFUUHYXC4IMFGXNBFDS5TMBB545Q6MON3EXYXHDOEJWU2LD2P"),
          counterAmount = IssuedAmount(
            units = 100000L,
            asset = IssuedAsset4("SLT", KeyPair.fromAccountId("GCKA6K5PCQ6PNF5RQBF7PQDJWRHO6UOGFMRLK3DYHDOI244V47XKQ4GP"))
          ),
          baseIsSeller = false)
      ).awaitFor(10.seconds)
    }

    "filter trades by offer id" >> {
      PublicNetwork.tradesByOfferId(283606L).map(_.take(10).last) must beEqualTo(
        Trade(
          id = "3748308153536513-0",
          ledgerCloseTime = ZonedDateTime.parse("2015-11-18T16:59:37Z"),
          offerId = 59L,
          baseAccount = KeyPair.fromAccountId("GAVH5JM5OKXGMQDS7YPRJ4MQCPXJUGH26LYQPQJ4SOMOJ4SXY472ZM7G"),
          baseAmount = Amount.lumens(30),
          counterAccount = KeyPair.fromAccountId("GBB4JST32UWKOLGYYSCEYBHBCOFL2TGBHDVOMZP462ET4ZRD4ULA7S2L"),
          counterAmount = IssuedAmount(
            units = 90000000L,
            asset = IssuedAsset4("JPY", KeyPair.fromAccountId("GBVAOIACNSB7OVUXJYC5UE2D4YK2F7A24T7EE5YOMN4CE6GCHUTOUQXM"))
          ),
          baseIsSeller = true)
      ).awaitFor(10.seconds)
    }
  }

  "transaction" should {
    "be accepted when posted to the network" >> {
      implicit val network = TestNetwork

      val newAccount = KeyPair.random
      val balance = for {
        sequence <- network.account(accn).map(_.lastSequence + 1)
        txn <- Future.fromTry {
          Transaction(Account(accn, sequence))
            .add(CreateAccountOperation(newAccount))
            .sign(accn)
        }
        _ <- txn.submit
        newBalances <- network.account(newAccount).map(_.balances)
      } yield {
        newBalances
      }

      balance must beEqualTo(Seq(Amount.lumens(1))).awaitFor(10.seconds)
    }
  }

}
