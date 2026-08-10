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
 * Adapted from ImageToolbox feature/delete-exif ExifRemovalPresetTest.kt
 * (see docs/adr/0004-imagetoolbox-ports.md); assertions extended for the
 * tactos MetadataTag enum.
 */
package dev.tactos.core.images.exif

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ExifRemovalPresetTest {

    @Test
    fun locationPresetContainsOnlyGpsTags() {
        val tags = ExifRemovalPreset.LocationOnly.tags

        assertTrue(tags.isNotEmpty())
        assertTrue(tags.all { it.key.startsWith("GPS") })
    }

    @Test
    fun privacyPresetContainsSensitiveIdentityAndLocationTags() {
        val tags = ExifRemovalPreset.Privacy.tags

        assertTrue(MetadataTag.GpsLatitude in tags)
        assertTrue(MetadataTag.CameraOwnerName in tags)
        assertTrue(MetadataTag.BodySerialNumber in tags)
        assertTrue(MetadataTag.DatetimeOriginal in tags)
        assertTrue(MetadataTag.MakerNote in tags)
    }

    @Test
    fun privacyPresetHasNoDuplicates() {
        val tags = ExifRemovalPreset.Privacy.tags
        assertTrue(tags.size == tags.distinct().size)
    }

    @Test
    fun keepDateAndCopyrightPresetDoesNotRemovePreservedTags() {
        val tags = ExifRemovalPreset.KeepDateAndCopyright.tags

        assertFalse(MetadataTag.DatetimeOriginal in tags)
        assertFalse(MetadataTag.Copyright in tags)
        assertTrue(MetadataTag.GpsLatitude in tags)
        assertTrue(MetadataTag.Make in tags)
    }

    @Test
    fun allMetadataAndCustomHaveEmptyListsByContract() {
        // AllMetadata = "engine clears every removable tag"; Custom = caller-
        // supplied. Both signal that via an empty tags list.
        assertTrue(ExifRemovalPreset.AllMetadata.tags.isEmpty())
        assertTrue(ExifRemovalPreset.Custom.tags.isEmpty())
    }

    @Test
    fun everyTagKeyIsUniqueAndNonBlank() {
        val keys = MetadataTag.entries.map { it.key }
        assertTrue(keys.all { it.isNotBlank() })
        assertTrue(keys.size == keys.distinct().size)
    }
}
