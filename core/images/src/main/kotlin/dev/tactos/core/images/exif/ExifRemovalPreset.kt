/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Adapted from ImageToolbox feature/delete-exif
 * domain/model/ExifRemovalPreset.kt for tactos (see
 * docs/adr/0004-imagetoolbox-ports.md): preset semantics unchanged, backed
 * by tactos's reduced MetadataTag enum instead of the upstream sealed class.
 */
package dev.tactos.core.images.exif

/**
 * What to strip. [tags] is the removal list; for [AllMetadata] and [Custom]
 * it is empty by contract — AllMetadata means "clear every removable tag"
 * (the engine iterates the whole enum) and Custom's list is user-supplied.
 */
enum class ExifRemovalPreset {
    AllMetadata,
    Privacy,
    LocationOnly,
    KeepDateAndCopyright,
    Custom;

    val tags: List<MetadataTag>
        get() = when (this) {
            AllMetadata -> emptyList()
            Privacy -> privacyTags
            LocationOnly -> locationTags
            KeepDateAndCopyright -> MetadataTag.entries.filterNot {
                it in MetadataTag.dateEntries || it == MetadataTag.Copyright
            }

            Custom -> emptyList()
        }

    companion object {
        private val locationTags by lazy {
            MetadataTag.entries.filter { it.key.startsWith("GPS") }
        }

        private val privacyTags by lazy {
            (locationTags + MetadataTag.dateEntries + listOf(
                MetadataTag.ImageDescription,
                MetadataTag.Make,
                MetadataTag.Model,
                MetadataTag.Software,
                MetadataTag.Artist,
                MetadataTag.Copyright,
                MetadataTag.MakerNote,
                MetadataTag.UserComment,
                MetadataTag.SubjectLocation,
                MetadataTag.DeviceSettingDescription,
                MetadataTag.ImageUniqueId,
                MetadataTag.CameraOwnerName,
                MetadataTag.BodySerialNumber,
                MetadataTag.LensMake,
                MetadataTag.LensModel,
                MetadataTag.LensSerialNumber,
            )).distinct()
        }
    }
}
