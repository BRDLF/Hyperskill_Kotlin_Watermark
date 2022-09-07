package watermark

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

object WaterMarker {

    class Apply {
        private val inputImage: BufferedImage = validImage()
        private val watermarkImage: BufferedImage = validWatermark()
        private val usingAlpha: Boolean = usingAlpha()
        private val transparencyColor: Color? = setTransparency()
        private val watermarkWeight: Int = validWeight()
        private val applicationMethod: String = validMethod()
        private val watermarkPosition: List<Int> = validPosition()
        private val outputFilename: String = validOutput()
        init {
            addWatermark()
        }

        private fun validImage(): BufferedImage {
            try {
                println("Input the image filename:")
                val fileName = readln()
                val fileFile = File(fileName)
                if (!fileFile.exists()) throw java.lang.RuntimeException("The file $fileName doesn't exist.")
                val image: BufferedImage = ImageIO.read(fileFile)
                if (image.colorModel.numColorComponents != 3) throw RuntimeException("The number of image color components isn't 3.")
                if (image.colorModel.pixelSize != 24 && image.colorModel.pixelSize != 32) throw RuntimeException("The image isn't 24 or 32-bit.")
                return image
            } catch (e: RuntimeException) {
                println(e.message)
                exitProcess(1)
            }
        }
        private fun validWatermark(): BufferedImage {
            try {
                println("Input the watermark image filename:")
                val fileName = readln()
                val fileFile = File(fileName)
                if (!fileFile.exists()) throw java.lang.RuntimeException("The file $fileName doesn't exist.")
                val watermark: BufferedImage = ImageIO.read(fileFile)
                if (watermark.colorModel.numColorComponents != 3) throw RuntimeException("The number of watermark color components isn't 3.")
                if (watermark.colorModel.pixelSize != 24 && watermark.colorModel.pixelSize != 32) throw RuntimeException(
                    "The watermark isn't 24 or 32-bit."
                )
                if (watermark.width > inputImage.width || watermark.height > inputImage.height) throw RuntimeException("The watermark's dimensions are larger.")
                return watermark
            } catch (e: RuntimeException) {
                println(e.message)
                exitProcess(2)
            }
        }
        private fun usingAlpha(): Boolean {
            if (watermarkImage.transparency == 3) {
                println("Do you want to use the watermark's Alpha channel?")
                return readln().lowercase() == "yes"
            }
            return false
        }
        private fun setTransparency(): Color? {
            if (watermarkImage.transparency != 3) {
                println("Do you want to set a transparency color?")
                if (readln().lowercase() == "yes") {
                    println("Input a transparency color ([Red] [Green] [Blue]):")
                    try {
                        val transparencyInput = readln().split(" ").map {it.toIntOrNull()?: -1}
                        if (transparencyInput.size != 3 ||
                            transparencyInput[0] !in 0..255 ||
                            transparencyInput[1] !in 0..255 ||
                            transparencyInput[2] !in 0..255) throw RuntimeException("The transparency color input is invalid.")
                        return Color(transparencyInput[0], transparencyInput[1], transparencyInput[2])
                    } catch (e: RuntimeException) {
                        println(e.message)
                        exitProcess(3)
                    }
                }
            }
            return null
        }
        private fun validWeight(): Int {
            try {
                println("Input the watermark transparency percentage (Integer 0-100):")
                val returnWeight = readln().toIntOrNull()
                    ?: throw RuntimeException("The transparency percentage isn't an integer number.")
                if (returnWeight !in 0..100) throw RuntimeException("The transparency percentage is out of range.")
                return returnWeight
            } catch (e: RuntimeException) {
                println(e.message)
                exitProcess(4)
            }
        }
        private fun validMethod(): String {
            println("Choose the position method (single, grid):")
            try {
                val inputMethod = readln()
                if (inputMethod != "single" && inputMethod != "grid") throw RuntimeException("The position method input is invalid.")
                return inputMethod
            } catch (e: RuntimeException) {
                println(e.message)
                exitProcess(5)
            }
        }
        private fun validOutput(): String {
            try {
                println("Input the output image filename (jpg or png extension):")
                val outputFilename = readln()
                if (!outputFilename.endsWith(".jpg") && !outputFilename.endsWith(".png")) throw RuntimeException("The output file extension isn't \"jpg\" or \"png\".")
                return outputFilename
            } catch (e: RuntimeException) {
                println(e.message)
                exitProcess(6)
            }
        }
        private fun validPosition(): List<Int> {
            if (applicationMethod != "single") return emptyList()
            val maxX = inputImage.width - watermarkImage.width
            val maxY = inputImage.height - watermarkImage.height
            println("Input the watermark position ([x 0-$maxX] [y 0-$maxY]):")
            try {
                val positionInput = readln().split(" ").map {it.toIntOrNull()?: throw RuntimeException("The position input is invalid.")}
                if (positionInput.size != 2) throw RuntimeException("The position input is invalid.")
                if (positionInput[0] !in 0..maxX || positionInput[1] !in 0..maxY) throw RuntimeException("The position input is out of range.")
                return positionInput
            } catch (e: RuntimeException) {
                print(e.message)
                exitProcess(7)
            }
        }
        private fun addWatermark() {
            when (applicationMethod) {
                "single" -> singleWatermark()
                "grid" -> gridWatermark()
                else -> {
                    println("Unexpected error in addWatermark()")
                    exitProcess(8)
                }
            }
            println("The watermarked image $outputFilename has been created.")
        }

        private fun singleWatermark() {
            val outputImage = inputImage.copy()
            outputImage.addWatermark(watermarkPosition[0], watermarkPosition[1])
            ImageIO.write(outputImage, outputFilename.substringAfterLast("."), File(outputFilename))
        }
        private fun gridWatermark() {
            val outputImage = inputImage.copy()
            for (x in 0..inputImage.width step watermarkImage.width) {
                for (y in 0..inputImage.height step watermarkImage.height) {
                    outputImage.addWatermark(x, y)
                }
            }
            ImageIO.write(outputImage, outputFilename.substringAfterLast("."), File(outputFilename))
        }

        private fun BufferedImage.addWatermark(initX: Int, initY: Int) {
            loopX@ for (x in 0 until watermarkImage.width) {
                loopY@ for (y in 0 until watermarkImage.height) {
                    if (initY + y >= inputImage.height) break@loopY
                    if (initX + x >= inputImage.width) break@loopX

                    val i = Color(inputImage.getRGB(initX + x, initY + y))
                    val w = Color(watermarkImage.getRGB(x, y), (usingAlpha))
                    val transparencyMatches = (((transparencyColor != null) && (w.red == transparencyColor.red) && (w.blue == transparencyColor.blue) && (w.green == transparencyColor.green)))
                    val pixelColor = when (w.alpha) {
                        0 -> i
                        else -> if (transparencyMatches) i else Color(
                            (watermarkWeight * w.red + (100 - watermarkWeight) * i.red) / 100,
                            (watermarkWeight * w.green + (100 - watermarkWeight) * i.green) / 100,
                            (watermarkWeight * w.blue + (100 - watermarkWeight) * i.blue) / 100
                        )
                    }
                    this.setRGB(initX + x, initY + y, pixelColor.rgb)
                }
            }
        }
        private fun BufferedImage.copy(): BufferedImage {
            val copiedImage = BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB)
            for (x in 0 until this.width) {
                for (y in 0 until this.height) {
                    copiedImage.setRGB(x, y, this.getRGB(x, y))
                }
            }
            return copiedImage
        }
    }
}

fun main() {
    WaterMarker.Apply()
}