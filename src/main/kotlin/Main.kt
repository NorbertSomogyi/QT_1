import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.apache.commons.math3.stat.StatUtils
import java.io.File
import kotlin.math.pow
import kotlin.math.sqrt

fun main(args: Array<String>) {
    val interArrivalTimes = mutableListOf<Double>()

    val data = mutableListOf<Double>()
    csvReader().open("src/main/resources/data.csv") {
        readAllAsSequence().forEach { row ->
            data.add(row[0].toDouble())
        }
    }

    data.sort()
    for (i in 0..data.size - 2) {
        interArrivalTimes.add(data[i + 1] - data[i])
    }

    val oneHour = data[data.size - 1]

    println("PMR: ${calculatePMR(data, oneHour / 3600, oneHour)}")
    println("SCV: ${calculateSCV(interArrivalTimes.toDoubleArray())}")
    println("M3: ${calculateM3(interArrivalTimes.toDoubleArray())}")
    val idi = calculateIDI(data)
    File("src/main/resources/IDI_Out").printWriter().use { out ->
        idi.forEach {
            out.println("${it.second}")
        }
    }
}

fun calculatePMR(data: List<Double>, oneSecond: Double, oneHour: Double): Double {
    var elapsedTime = 0.0
    val trafficList = mutableListOf<Double>()
    while (elapsedTime < oneHour) {
        val traffic = data.filter { it > elapsedTime - oneSecond && it <= elapsedTime }.size
        trafficList.add(traffic.toDouble())
        elapsedTime += oneSecond
    }
    val trafficArray = trafficList.toDoubleArray()
    return StatUtils.max(trafficArray) / StatUtils.mean(trafficArray)
}

fun calculateSCV(interArrivalTimes: DoubleArray) =
    StatUtils.variance(interArrivalTimes) / (StatUtils.mean(interArrivalTimes).pow(2))

fun calculateM3(interArrivalTimes: DoubleArray): Double {
    val thirdArrivalTimes = mutableListOf<Double>()
    val squareArrivalTimes = mutableListOf<Double>()
    for (time in interArrivalTimes) {
        thirdArrivalTimes.add(time.pow(3))
        squareArrivalTimes.add(time.pow(2))
    }

    val standardDeviation =
        sqrt(StatUtils.mean(squareArrivalTimes.toDoubleArray()) - StatUtils.mean(interArrivalTimes).pow(2))
    return (StatUtils.mean(thirdArrivalTimes.toDoubleArray()) - (3 * StatUtils.mean(interArrivalTimes) * standardDeviation.pow(
        2
    ))
            - (StatUtils.mean(interArrivalTimes).pow(3))) / standardDeviation.pow(3)
}

fun calculateIDI(interArrivalSequence: List<Double>): List<Pair<Double, Double>> {
    val xi = mutableListOf<Double>()
    val indexMin = 25000
    val indexMax = 375000

    interArrivalSequence.forEachIndexed { index, arrival ->
        if (index in indexMin..indexMax) {
            xi.add(arrival)
        }
    }

    val maxK = 10000
    var k = 1
    val result = mutableListOf<Pair<Double, Double>>()

    val lastIndex = interArrivalSequence.lastIndexOf(xi[xi.size - 1])
    while (k <= maxK) {
        val X = mutableListOf<Double>()
        for (i in lastIndex + 1..lastIndex + k) {
            X.add(interArrivalSequence[i])
        }
        val xArray = X.toDoubleArray()
        result.add(Pair(k.toDouble(), StatUtils.variance(xArray) / (k * (StatUtils.mean(xArray).pow(2)))))
        k++
    }

    return result
}

