package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    private const val TAG = "PdfExporter"

    /**
     * Generates a premium PDF of the viral generation package.
     * Uses Standard Android PDF API.
     */
    fun exportToPdf(context: Context, jsonString: String, outputStream: OutputStream): Boolean {
        val pdfDocument = PdfDocument()

        try {
            val json = JSONObject(jsonString)
            val topic = json.optString("topic", "ReelForge Video")
            val platform = json.optString("platform", "Unknown")
            val niche = json.optString("niche", "General")
            val tone = json.optString("tone", "Cinematic")
            val language = json.optString("language", "English")
            val duration = json.optString("duration", "30s")
            val viralityScore = json.optInt("viralityScore", 90)
            val retentionScore = json.optInt("retentionScore", 85)
            val concept = json.optString("storyboardingConcept", "")

            // Paints setup
            val textPaint = Paint().apply {
                color = Color.rgb(200, 200, 205)
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(255, 255, 255)
                textSize = 22f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(0, 230, 200) // Teal accent
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val metaHeaderPaint = Paint().apply {
                color = Color.rgb(240, 240, 240)
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
            }

            val backgroundPaint = Paint().apply {
                color = Color.rgb(18, 19, 23) // Slate-charcoal dark theme
            }

            // PAGE 1: COVER PAGE
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Fill Dark Background
            canvas.drawRect(0f, 0f, 595f, 842f, backgroundPaint)

            // Header Glow Line
            val topBarPaint = Paint().apply { color = Color.rgb(0, 230, 200) }
            canvas.drawRect(0f, 0f, 595f, 8f, topBarPaint)

            // Cover Layout drawing
            var currentY = 150f
            canvas.drawText("REELFORGE AI", 50f, currentY, titlePaint.apply { textSize = 28f })
            currentY += 40f
            
            val linePaint = Paint().apply {
                color = Color.rgb(40, 42, 50)
                strokeWidth = 2f
            }
            canvas.drawLine(50f, currentY, 545f, currentY, linePaint)
            currentY += 40f

            canvas.drawText("VIRAL PRODUCTION ASSETS PACKAGE", 50f, currentY, subtitlePaint.apply { textSize = 14f })
            currentY += 50f

            canvas.drawText("PROJECT DESCRIPTION:", 50f, currentY, metaHeaderPaint)
            currentY += 25f

            // Drawing wrapped description
            currentY = drawWrappedText(canvas, "\"$topic\"", 50f, currentY, 495f, textPaint.apply { 
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            }) + 40f

            canvas.drawText("PRODUCTION METADATA:", 50f, currentY, metaHeaderPaint.apply { textSize = 12f; typeface = Typeface.DEFAULT_BOLD })
            currentY += 25f

            val metadataRows = listOf(
                "Platform:" to platform,
                "Content Niche:" to niche,
                "Tone & Style:" to tone,
                "Language:" to language,
                "Duration:" to duration,
                "Virality Index:" to "$viralityScore%",
                "Retention Pacing:" to "$retentionScore%"
            )

            for ((key, value) in metadataRows) {
                canvas.drawText(key, 50f, currentY, textPaint.apply { typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(160, 160, 165) })
                canvas.drawText(value, 200f, currentY, textPaint.apply { typeface = Typeface.DEFAULT; color = Color.WHITE })
                currentY += 22f
            }

            currentY += 40f
            canvas.drawLine(50f, currentY, 545f, currentY, linePaint)
            currentY += 40f

            val exportDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
            canvas.drawText("Export Date: $exportDate", 50f, currentY, textPaint.apply { color = Color.rgb(120, 120, 125); textSize = 10f })
            canvas.drawText("Generated via ReelForge AI Client Pro", 50f, currentY + 18f, textPaint)

            pdfDocument.finishPage(page)

            // PAGE 2: HOOKS & OVERVIEW CONCEPT
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawRect(0f, 0f, 595f, 842f, backgroundPaint)
            
            currentY = 50f
            canvas.drawText("1. OVERVIEW & STRATEGIC HOOKS", 50f, currentY, subtitlePaint.apply { textSize = 14f })
            currentY += 25f

            if (concept.isNotEmpty()) {
                canvas.drawText("STORYBOARDING CONCEPT:", 50f, currentY, metaHeaderPaint)
                currentY += 20f
                currentY = drawWrappedText(canvas, concept, 50f, currentY, 495f, textPaint.apply { color = Color.rgb(200, 200, 205); textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) }) + 35f
            }

            canvas.drawText("VIRAL ATTRACTOR HOOKS (5 TYPES):", 50f, currentY, metaHeaderPaint)
            currentY += 25f

            val hooks = json.optJSONArray("hooks")
            if (hooks != null && hooks.length() > 0) {
                for (i in 0 until hooks.length()) {
                    val hookObj = hooks.optJSONObject(i) ?: continue
                    val hookType = hookObj.optString("type", "Hook")
                    val hookText = hookObj.optString("text", "")

                    if (currentY > 750f) {
                        pdfDocument.finishPage(page)
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawRect(0f, 0f, 595f, 842f, backgroundPaint)
                        currentY = 50f
                    }

                    // Background accent card for hooks
                    val cardPaint = Paint().apply { color = Color.rgb(26, 27, 33) }
                    canvas.drawRoundRect(50f, currentY - 12f, 545f, currentY + 45f, 6f, 6f, cardPaint)

                    canvas.drawText("$hookType Hook:", 60f, currentY + 5f, textPaint.apply { color = Color.rgb(0, 230, 200); typeface = Typeface.DEFAULT_BOLD })
                    currentY = drawWrappedText(canvas, hookText, 60f, currentY + 22f, 465f, textPaint.apply { color = Color.WHITE; typeface = Typeface.DEFAULT }) + 25f
                    currentY += 30f
                }
            }

            pdfDocument.finishPage(page)

            // PAGE 3+: SCENE-BY-SCENE COMPLETE SCRIPT TIMELINE
            var scenePageNum = pdfDocument.pages.size + 1
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, scenePageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawRect(0f, 0f, 595f, 842f, backgroundPaint)

            currentY = 50f
            canvas.drawText("2. PRODUCTION TIMELINE & SCRIPT", 50f, currentY, subtitlePaint)
            currentY += 30f

            val scenes = json.optJSONArray("scenes")
            if (scenes != null && scenes.length() > 0) {
                for (s in 0 until scenes.length()) {
                    val sceneObj = scenes.optJSONObject(s) ?: continue
                    val sceneNum = sceneObj.optInt("sceneNumber", s + 1)
                    val dur = sceneObj.optString("duration", "3s")
                    val purpose = sceneObj.optString("purpose", "Scene")
                    val intensity = sceneObj.optInt("emotionalIntensity", 5)
                    val narration = sceneObj.optString("narration", "")
                    val imgPrompt = sceneObj.optString("imagePrompt", "")
                    val animPrompt = sceneObj.optString("animationPrompt", "")

                    // Calculate height required for this scene
                    val estimateHeight = 180f // estimated block size
                    if (currentY + estimateHeight > 780f) {
                        pdfDocument.finishPage(page)
                        scenePageNum++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, scenePageNum).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawRect(0f, 0f, 595f, 842f, backgroundPaint)
                        currentY = 50f
                        canvas.drawText("PRODUCTION TIMELINE & SCRIPT (CONTINUED)", 50f, currentY, subtitlePaint)
                        currentY += 30f
                    }

                    // Scene Card Header
                    val sceneHeaderPaint = Paint().apply { color = Color.rgb(31, 32, 40) }
                    canvas.drawRoundRect(50f, currentY - 5f, 545f, currentY + 22f, 4f, 4f, sceneHeaderPaint)

                    canvas.drawText("SCENE #$sceneNum • Duration: $dur • Purpose: $purpose • Intensity: $intensity/10", 60f, currentY + 12f, textPaint.apply { color = Color.rgb(0, 230, 200); typeface = Typeface.DEFAULT_BOLD })
                    currentY += 35f

                    // Narration Spoken Lines
                    canvas.drawText("NARRATION (SPOKEN SCRIPT):", 55f, currentY, textPaint.apply { color = Color.rgb(170, 170, 175); typeface = Typeface.DEFAULT_BOLD })
                    currentY += 15f
                    currentY = drawWrappedText(canvas, narration, 55f, currentY, 480f, textPaint.apply { color = Color.WHITE; typeface = Typeface.DEFAULT }) + 20f

                    // Image Prompt
                    canvas.drawText("CINEMATIC VISUAL PROMPT (9:16):", 55f, currentY, textPaint.apply { color = Color.rgb(170, 170, 175); typeface = Typeface.DEFAULT_BOLD })
                    currentY += 15f
                    currentY = drawWrappedText(canvas, imgPrompt, 55f, currentY, 480f, textPaint.apply { color = Color.rgb(190, 190, 195); typeface = Typeface.DEFAULT }) + 20f

                    // Animation Prompt
                    canvas.drawText("ANIMATION DIRECTION:", 55f, currentY, textPaint.apply { color = Color.rgb(170, 170, 175); typeface = Typeface.DEFAULT_BOLD })
                    currentY += 15f
                    currentY = drawWrappedText(canvas, animPrompt, 55f, currentY, 480f, textPaint.apply { color = Color.rgb(180, 210, 220); typeface = Typeface.DEFAULT }) + 25f

                    // Border divider between scenes
                    canvas.drawLine(50f, currentY, 545f, currentY, linePaint)
                    currentY += 20f
                }
            }

            pdfDocument.finishPage(page)

            // PAGE 4 / FINAL: SEO & SEARCHABILITY PACKAGES
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawRect(0f, 0f, 595f, 842f, backgroundPaint)

            currentY = 50f
            canvas.drawText("3. PLATFORM-OPTIMIZED SEO & CAPTIONS", 50f, currentY, subtitlePaint)
            currentY += 30f

            val seoObj = json.optJSONObject("seo")
            if (seoObj != null) {
                // SEO TITLE
                canvas.drawText("VIRAL SEO TITLE:", 50f, currentY, metaHeaderPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, seoObj.optString("title", ""), 50f, currentY, 495f, textPaint.apply { color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD }) + 25f

                // SEO DESCRIPTION
                canvas.drawText("SEO KEYWORDS & SEARCH DESCRIPTION:", 50f, currentY, metaHeaderPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, seoObj.optString("description", ""), 50f, currentY, 495f, textPaint.apply { color = Color.rgb(190, 190, 195); typeface = Typeface.DEFAULT }) + 25f

                // PLATFORM CAPTION
                canvas.drawText("READY-TO-PASTE CAPTION & HOOK:", 50f, currentY, metaHeaderPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, seoObj.optString("caption", ""), 50f, currentY, 495f, textPaint.apply { color = Color.WHITE; typeface = Typeface.DEFAULT }) + 25f

                // HASHTAGS
                canvas.drawText("VIRAL HASHTAG ENGINE PLUGS:", 50f, currentY, metaHeaderPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, seoObj.optString("hashtags", ""), 50f, currentY, 495f, textPaint.apply { color = Color.rgb(0, 230, 200); typeface = Typeface.DEFAULT_BOLD }) + 25f

                // CALLS TO ACTION
                canvas.drawText("RETENTION CTAS (CONVERSIONS):", 50f, currentY, metaHeaderPaint)
                currentY += 18f
                currentY = drawWrappedText(canvas, seoObj.optString("ctas", ""), 50f, currentY, 495f, textPaint.apply { color = Color.rgb(255, 120, 150); typeface = Typeface.DEFAULT_BOLD }) + 25f
            }

            // Footer branding on final page
            currentY = 770f
            canvas.drawLine(50f, currentY, 545f, currentY, linePaint)
            currentY += 20f
            canvas.drawText("ReelForge AI • Designed for shorts, reels, and tiktok faceless creators.", 50f, currentY, textPaint.apply { color = Color.rgb(120, 120, 125); textSize = 9f })

            pdfDocument.finishPage(page)

            // Write out to output stream
            pdfDocument.writeTo(outputStream)
            Log.d(TAG, "PDF generated successfully!")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile ReelForge PDF document", e)
            return false
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Splits long text into wrapped lines and draws them. Returns bottom Y coordinate.
     */
    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, width: Float, paint: Paint): Float {
        var currentY = startY
        val words = text.split(" ")
        var line = ""

        val textBoundsPaint = Paint(paint)

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val textWidth = textBoundsPaint.measureText(testLine)
            if (textWidth > width) {
                canvas.drawText(line, x, currentY, paint)
                currentY += paint.textSize * 1.4f
                line = word
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x, currentY, paint)
            currentY += paint.textSize * 1.4f
        }
        return currentY - (paint.textSize * 0.4f) // offset back
    }
}
