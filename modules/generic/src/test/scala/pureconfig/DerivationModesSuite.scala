package pureconfig

import com.typesafe.config.ConfigFactory
import shapeless.test.illTyped

class DerivationModesSuite extends BaseSuite {

  sealed trait Entity
  case class Person(name: String, surname: String) extends Entity
  case class Place(name: String, lat: Double, lon: Double) extends Entity

  val person = Person("John", "Doe")
  val conf = ConfigFactory.parseString("{ type: person, name: John, surname: Doe }")

  case class CustomCaseClass(
      customObject: CustomObject,
      mapCustomObject: Map[String, CustomObject],
      mapListCustomObject: Map[String, List[CustomObject]]
  )
  case class CustomObject(value: Int)
  object CustomObject {
    implicit val pureconfigReader: ConfigReader[CustomObject] = ConfigReader.fromStringOpt {
      case "eaaxacaca" => Some(CustomObject(453))
      case "a" => Some(CustomObject(45))
      case _ => Some(CustomObject(1))
    }
    implicit val pureconfigWriter: ConfigWriter[CustomObject] = ConfigWriter.toString {
      case CustomObject(453) => "eaaxacaca"
      case CustomObject(45) => "a"
      case _ => "cvbc"
    }
  }

  val customCaseClass = CustomCaseClass(
    CustomObject(453),
    Map("a" -> CustomObject(453), "b" -> CustomObject(45)),
    Map("x" -> List(CustomObject(45), CustomObject(453), CustomObject(1)))
  )
  val customConf = ConfigFactory.parseString("""{
      |  custom-object = "eaaxacaca"
      |  map-custom-object { a = "eaaxacaca", b = "a" }
      |  map-list-custom-object { x = ["a", "eaaxacaca", "cvbc"]}
      |}""".stripMargin)

  behavior of "default"

  it should "not provide instance derivation for products and coproducts out-of-the-box" in {
    illTyped("loadConfig[Entity](conf)")
    illTyped("ConfigWriter[Entity]")
  }

  behavior of "semiauto"

  it should "not provide instance derivation for products and coproducts out-of-the-box" in {
    illTyped("{ import pureconfig.generic.semiauto._; loadConfig[Entity](conf) }")
    illTyped("{ import pureconfig.generic.semiauto._; ConfigWriter[Entity] }")
  }

  it should "provide methods to derive readers on demand" in {
    import pureconfig.generic.semiauto._

    implicit val personReader = deriveReader[Person]
    implicit val placeReader = deriveReader[Place]
    implicit val entityReader = deriveReader[Entity]

    ConfigReader[Entity].from(conf.root) shouldBe Right(person)
  }

  it should "provide methods to derive writers on demand" in {
    import pureconfig.generic.semiauto._

    implicit val personWriter = deriveWriter[Person]
    implicit val placeWriter = deriveWriter[Place]
    implicit val entityWriter = deriveWriter[Entity]

    ConfigWriter[Entity].to(person) shouldBe conf.root()
  }

  it should "provide methods to derive full converters on demand" in {
    import pureconfig.generic.semiauto._

    implicit val personConvert = deriveConvert[Person]
    implicit val placeConvert = deriveConvert[Place]
    implicit val entityConvert = deriveConvert[Entity]

    ConfigReader[Entity].from(conf.root) shouldBe Right(person)
    ConfigWriter[Entity].to(person) shouldBe conf.root()
  }

  behavior of "auto"

  it should "provide instance derivation for products and coproducts out-of-the-box" in {
    import pureconfig.generic.auto._

    ConfigReader[Entity].from(conf.root) shouldBe Right(person)
    ConfigWriter[Entity].to(person) shouldBe conf.root()
  }

  it should "use existing reader and writer instances when they exist" in {
    import pureconfig.generic.auto._

    ConfigReader[CustomCaseClass].from(customConf.root) shouldBe Right(customCaseClass)
    ConfigWriter[CustomCaseClass].to(customCaseClass) shouldBe customConf.root()
  }
}
