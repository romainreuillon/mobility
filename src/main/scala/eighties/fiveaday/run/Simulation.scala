/**
  * Created by Romain Reuillon on 09/05/16.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  */
package eighties.fiveaday.run

import better.files._
import eighties.h24.generation._
import eighties.h24.space._
import eighties.fiveaday.observable
import eighties.fiveaday.opinion.interchangeConviction
import eighties.fiveaday.population.Individual
import eighties.fiveaday.health._
import eighties.h24.simulation._
import scala.util.Random

object Simulation {

  def run(
    maxProbaToSwitch: Double,
    constraintsStrength: Double,
    inertiaCoefficient: Double,
    healthyDietReward: Double,
    interpersonalInfluence: Double,
    days: Int,
    population: java.io.File,
    moves: java.io.File,
    distributionConstraints: java.io.File,
    moveType: MoveType,
    rng: Random) = {

    val healthCategory = generateHealthCategory(File(distributionConstraints.toURI))
    val interactionMap = generateInteractionMap(File(distributionConstraints.toURI))

    def buildIndividual(feature: IndividualFeature, random: Random) = Individual(feature, healthCategory, rng)

    def exchange(moved: World[Individual], slice: Int, rng: Random) = interchangeConviction(
      moved,
      slice,
      interactionMap,
      maxProbaToSwitch = maxProbaToSwitch,
      constraintsStrength = constraintsStrength,
      inertiaCoefficient = inertiaCoefficient,
      healthyDietReward = healthyDietReward,
      interpersonalInfluence = interpersonalInfluence,
      rng
    )

    simulate(days, population, moves, moveType, buildIndividual, exchange, Individual.stableDestinationsV, Individual.locationV, Individual.homeV, Individual.socialCategoryV.get, rng)
  }

}

object Fit {

  def fitness(world: World[Individual]) = observable.deltaHealth(world)

  def run(
    maxProbaToSwitch: Double,
    constraintsStrength: Double,
    inertiaCoefficient: Double,
    healthyDietReward: Double,
    interpersonalInfluence: Double,
    days: Int,
    population: java.io.File,
    moves: java.io.File,
    distributionConstraints: java.io.File,
    moveType: MoveType,
    rng: Random) = {

    fitness(
      Simulation.run(
        maxProbaToSwitch = maxProbaToSwitch,
        constraintsStrength = constraintsStrength,
        inertiaCoefficient = inertiaCoefficient,
        healthyDietReward = healthyDietReward,
        interpersonalInfluence = inertiaCoefficient,
        days = days,
        population = population,
        moves = moves,
        distributionConstraints = distributionConstraints,
        moveType = moveType,
        rng = rng
      )
    )

  }

  def loadMatrix(data: File) = data.lines.drop(1)
}

object SimulationApp extends App {

  val seed = 42
  val rng = new Random(seed)
  //val result = File("results")
  val generatedData = File("data")

//  val outputPath = result / "nomove"
//  outputPath.createDirectories

//  println(Calendar.getInstance.getTime + " loading population")
  val worldFeatures = generatedData / "population.bin"
  val moves = generatedData / "moves"
  val dataDirectory = File("../data/")
//  val pathEGT = dataDirectory / "EGT 2010/presence semaine EGT"
  val distributionConstraints = dataDirectory / "initialisation_distribution_per_cat_2002_2008.csv"

  val parameterSets =
    Vector(
      (0.8507843893208267,0.45377746673575825,0.6585498777924014,0.210784861364803,0.2589547233574915),
      (0.8391008302839391,0.4281592636895263,0.6548785686047478,0.2147412463304806,0.4204431246470749)
    )

  val (maxProbaToSwitch, constraintsStrength, inertiaCoefficient, healthyDietReward, interpersonalInfluence) = parameterSets(0)

  val world =
    Simulation.run(
      maxProbaToSwitch = maxProbaToSwitch,
      constraintsStrength = constraintsStrength,
      inertiaCoefficient = inertiaCoefficient,
      healthyDietReward = healthyDietReward,
      interpersonalInfluence = interpersonalInfluence,
      days = 6,
      population = worldFeatures.toJava,
      moves = moves.toJava,
      distributionConstraints = distributionConstraints.toJava,
      moveType = MoveType.Random,
      rng = rng
    )

  println("delta health: " + observable.deltaHealth(world))
  println("social inequality: " + observable.weightedInequalityRatioBySexAge(world))
  //println("world error: " + World.allIndividuals.getAll(world).count(_.healthy))

}

