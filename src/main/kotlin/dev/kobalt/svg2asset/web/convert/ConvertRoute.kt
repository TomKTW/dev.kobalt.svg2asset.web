/*
 * dev.kobalt.svg2asset
 * Copyright (C) 2022 Tom.K
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.kobalt.svg2asset.web.convert

import dev.kobalt.svg2asset.web.extension.*
import dev.kobalt.svg2asset.web.html.LimitedSizeInputStream
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import java.io.ByteArrayOutputStream

fun Route.convertRoute() {
    route("convert/") {
        get {
            call.respondHtmlContent(
                title = ConvertRepository.pageTitle,
                description = ConvertRepository.pageSubtitle
            ) {
                pageArticle(
                    ConvertRepository.pageTitle,
                    ConvertRepository.pageSubtitle
                ) {
                    h3 { text("Form") }
                    form {
                        method = FormMethod.post
                        encType = FormEncType.multipartFormData
                        div {
                            pageInputFile("Page file", "input")
                            br { }
                            pageInputRadio("Android", "type", "android")
                            pageInputRadio("iOS", "type", "ios")
                            br { }
                            pageInputSubmit("Begin conversion", "submit")
                        }
                    }
                    pageMarkdown(ConvertRepository.pageContent)
                }
            }
        }
        post {
            runCatching {
                val parts = call.receiveMultipart().readAllParts()
                val part = parts.find { it.name == "input" } as? PartData.FileItem ?: throw Exception()
                val type = parts.find { it.name == "type" } as? PartData.FormItem ?: throw Exception()
                val typeExtension = when (type.value) {
                    "android" -> "xml"; "ios" -> "pdf"; else -> throw Exception()
                }
                val inputFilename = part.originalFileName ?: "asset.svg"
                val outputFilename = inputFilename.substringBeforeLast(".svg") + "." + typeExtension
                val data = LimitedSizeInputStream(part.streamProvider(), 500 * 1024).readBytes().decodeToString()
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, outputFilename)
                        .toString()
                )
                call.respondBytes(
                    contentType = ContentType.Application.Zip,
                    status = HttpStatusCode.OK,
                    bytes = ByteArrayOutputStream().use {
                        ConvertRepository.submit(data, type.value, it)
                        it.toByteArray()
                    }
                )
            }.getOrElse {
                it.printStackTrace()
                call.respondHtmlContent(
                    title = ConvertRepository.pageTitle,
                    description = ConvertRepository.pageSubtitle
                ) {
                    pageArticle(
                        ConvertRepository.pageTitle,
                        ConvertRepository.pageSubtitle
                    ) {
                        h3 { text("Failure") }
                        p { text("Conversion process was not successful.") }
                        pageMarkdown(ConvertRepository.pageContent)
                    }
                }
            }
        }
    }
}

