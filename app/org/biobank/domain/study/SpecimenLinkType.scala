package org.biobank.domain.study

import org.biobank.domain.{
  ConcurrencySafeEntity,
  ContainerTypeId,
  DomainError,
  DomainValidation,
  HasUniqueName,
  HasDescriptionOption
}
import org.biobank.domain.validation.StudyValidationHelper

import scalaz._
import scalaz.Scalaz._

/** [[SpecimenLinkType]]s are assigned to a [[ProcessingType]], and are used to represent a regularly
  * performed processing procedure involving two [[Specimen]]s: an input, which must be in a specific
  * [[SpecimenGroup]], and an output, which must also be in another specific [[SpecimenGroup]].
  *
  * To avoid redundancy, each combination of inputGroup and outputGroup may exist only once per
  * ProcessingType.
  *
  * @param procesingTypeId the [[ProcessingType]] this specimen link belongs to.
  *
  * @param expectedInputChange the expected amount to be removed from each input. If the value is
  *        not required then use a value of zero.
  *
  * @param expectedOutputChange the expected amount to be added to each output. If the value is
  *        not required then use a value of zero.
  *
  * @param inputCount the number of expected specimens when the processing is carried out.
  *
  * @param outputCount are the number of resulting specimens when the processing is carried out.
  *        A value of zero for output count implies that the count is the same as the input count.
  *
  * @param inputGroupId The [[SpecimenGroup]] the input specimens are from.
  *
  * @param outputGroupId The [[SpecimenGroup]] the output specimens are from.
  *
  * @param inputContainerTypeId The specimen container type that holds the input specimens. This is
  *        an optional field.
  *
  * @param outputContainerTypeId The specimen container type that the output specimens are stored
  *        into. This is an optional field.
  */
case class SpecimenLinkType private (
  procesingTypeId: ProcessingTypeId,
  id: SpecimenLinkTypeId,
  version: Long,
  expectedInputChange: BigDecimal,
  expectedOutputChange: BigDecimal,
  inputCount: Int,
  outputCount: Int,
  inputGroupId: SpecimenGroupId,
  outputGroupId: SpecimenGroupId,
  inputContainerTypeId: Option[ContainerTypeId],
  outputContainerTypeId: Option[ContainerTypeId])
    extends ConcurrencySafeEntity[SpecimenLinkTypeId] {

  /** Updates a specimen link type with one or more new values.
    */
  def update(
    expectedVersion: Option[Long],
    expectedInputChange: BigDecimal,
    expectedOutputChange: BigDecimal,
    inputCount: Int,
    outputCount: Int,
    inputGroupId: SpecimenGroupId,
    outputGroupId: SpecimenGroupId,
    inputContainerTypeId: Option[ContainerTypeId],
    outputContainerTypeId: Option[ContainerTypeId]): DomainValidation[SpecimenLinkType] = {
    for {
      validVersion <- requireVersion(expectedVersion)
      newItem <- SpecimenLinkType.create(procesingTypeId, id, version,  expectedInputChange,
        expectedOutputChange, inputCount, outputCount, inputGroupId, outputGroupId,
        inputContainerTypeId, outputContainerTypeId)
    } yield newItem
  }

  override def toString: String =
    s"""|CollectionEventType:{
        |  procesingTypeId: $procesingTypeId,
        |  id: $id,
        |  version: $version,
        |  expectedInputChange, $expectedInputChange,
        |  expectedOutputChange, $expectedOutputChange,
        |  inputCount, $inputCount,
        |  outputCount, $outputCount,
        |  inputGroupId, $inputGroupId,
        |  outputGroupId, $outputGroupId,
        |  inputContainerTypeId, $inputContainerTypeId,
        |  outputContainerTypeId, $outputContainerTypeId
        |}""".stripMargin
}

object SpecimenLinkType extends StudyValidationHelper {

  def create(
    procesingTypeId: ProcessingTypeId,
    id: SpecimenLinkTypeId,
    version: Long,
    expectedInputChange: BigDecimal,
    expectedOutputChange: BigDecimal,
    inputCount: Int,
    outputCount: Int,
    inputGroupId: SpecimenGroupId,
    outputGroupId: SpecimenGroupId,
    inputContainerTypeId: Option[ContainerTypeId],
    outputContainerTypeId: Option[ContainerTypeId]): DomainValidation[SpecimenLinkType] = {

    (validateId(procesingTypeId).toValidationNel |@|
      validateId(id).toValidationNel |@|
      validateAndIncrementVersion(version).toValidationNel |@|
      validatePositiveNumber(
	expectedInputChange,
	"expected input change is not a positive number").toValidationNel |@|
      validatePositiveNumber(
	expectedOutputChange,
	"expected output change is not a positive number").toValidationNel |@|
      validatePositiveNumber(
	inputCount,
	"input count is not a positive number").toValidationNel |@|
      validatePositiveNumber(
	outputCount,
	"output count is not a positive number").toValidationNel |@|
      validateId(inputGroupId).toValidationNel |@|
      validateId(outputGroupId).toValidationNel |@|
      validateId(inputContainerTypeId).toValidationNel |@|
      validateId(outputContainerTypeId).toValidationNel) {
      SpecimenLinkType(_, _, _, _, _, _, _, _, _, _, _)
    }

  }

  protected def validateId(id: SpecimenLinkTypeId): Validation[String, SpecimenLinkTypeId] = {
    validateStringId(id.toString, "collection event type id is null or empty") match {
      case Success(idString) => id.success
      case Failure(err) => err.fail
    }
  }
}
