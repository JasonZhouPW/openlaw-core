package org.adridadou.openlaw.parser.template.expressions

import cats.implicits._
import io.circe._
import org.adridadou.openlaw.OpenlawValue
import org.adridadou.openlaw.parser.template.variableTypes.VariableType
import org.adridadou.openlaw.parser.template._
import org.adridadou.openlaw.result.Result

import scala.reflect.ClassTag

trait Expression {
  def missingInput(
      executionResult: TemplateExecutionResult
  ): Result[List[VariableName]]

  def validate(executionResult: TemplateExecutionResult): Result[Unit]

  def minus(
      right: Expression,
      executionResult: TemplateExecutionResult
  ): Result[Option[OpenlawValue]] =
    for {
      exprType <- expressionType(executionResult)
      result <- exprType.minus(this, right, executionResult)
    } yield result

  def plus(
      right: Expression,
      executionResult: TemplateExecutionResult
  ): Result[Option[OpenlawValue]] =
    for {
      exprType <- expressionType(executionResult)
      result <- exprType.plus(this, right, executionResult)
    } yield result

  def multiply(
      right: Expression,
      executionResult: TemplateExecutionResult
  ): Result[Option[OpenlawValue]] =
    for {
      exprType <- expressionType(executionResult)
      result <- exprType.multiply(this, right, executionResult)
    } yield result

  def divide(
      right: Expression,
      executionResult: TemplateExecutionResult
  ): Result[Option[OpenlawValue]] =
    for {
      exprType <- expressionType(executionResult)
      result <- exprType.divide(this, right, executionResult)
    } yield result

  def expressionType(
      executionResult: TemplateExecutionResult
  ): Result[VariableType]
  def evaluate(
      executionResult: TemplateExecutionResult
  ): Result[Option[OpenlawValue]]
  def evaluateT[U <: OpenlawValue](
      executionResult: TemplateExecutionResult
  )(implicit classTag: ClassTag[U]): Result[Option[U#T]] =
    evaluate(executionResult).flatMap(_.map(VariableType.convert[U]).sequence)

  def variables(
      executionResult: TemplateExecutionResult
  ): Result[List[VariableName]]
}

final case class ParensExpression(expr: Expression) extends Expression {
  override def missingInput(
      executionResult: TemplateExecutionResult
  ): Result[List[VariableName]] =
    expr.missingInput(executionResult)

  override def validate(
      executionResult: TemplateExecutionResult
  ): Result[Unit] =
    expr.validate(executionResult)

  override def expressionType(
      executionResult: TemplateExecutionResult
  ): Result[VariableType] =
    expr.expressionType(executionResult)

  override def evaluate(
      executionResult: TemplateExecutionResult
  ): Result[Option[OpenlawValue]] =
    expr.evaluate(executionResult)

  override def variables(
      executionResult: TemplateExecutionResult
  ): Result[List[VariableName]] =
    expr.variables(executionResult)

  override def toString: String = s"($expr)"
}

object Expression {
  private val exprParser = new ExpressionParserService()

  implicit val exprEnc: Encoder[Expression] = (a: Expression) =>
    Json.fromString(a.toString)
  implicit val exprDec: Decoder[Expression] = (c: HCursor) =>
    c.as[String].flatMap(parseExpression)

  private def parseExpression(
      value: String
  ): Either[DecodingFailure, Expression] =
    exprParser.parseExpression(value) match {
      case Right(expr) =>
        Right(expr)
      case Left(ex) =>
        Left(DecodingFailure(ex.message, Nil))
    }
}
