package com.xtgxiso.autoledger.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 本地 OCR 引擎：用 ML Kit 的中文文字识别，离线、免费、图片不出手机。
 * 只做「从图片提取文字」这一件事，不负责理解账目（解析在 OcrBillParser 里）。
 */
object OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /**
     * 把图片路径识别成「按行分组的文字」。
     * 返回每行一个字符串（按识别结果里 block/line 的顺序），方便后续解析账目。
     */
    suspend fun recognize(imagePath: String): List<String> = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmap(imagePath) ?: return@withContext emptyList()
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result: Text = recognizer.process(image).await()
            val out = ArrayList<String>()
            val blocks: List<Text.TextBlock> = result.textBlocks
            for (block in blocks) {
                val lines: List<Text.Line> = block.lines
                for (line in lines) {
                    val t = line.text?.trim().orEmpty()
                    if (t.isNotEmpty()) out.add(t)
                }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun decodeBitmap(path: String): Bitmap? = runCatching {
        BitmapFactory.decodeFile(path)
    }.getOrNull()
}
