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
 * Adapted from ImageToolbox core/domain image/model/MetadataTag.kt for
 * tactos (see docs/adr/0004-imagetoolbox-ports.md): the sealed-class
 * hierarchy of ~150 tags is reduced to an enum of the user-removable tags
 * tactos strips, keyed by the EXIF tag strings androidx ExifInterface uses.
 * Structural tags (orientation, dimensions, color space) are deliberately
 * excluded so stripping never visually corrupts an image.
 */
package dev.tactos.core.images.exif

/**
 * A removable EXIF tag. [key] is the exact string androidx
 * `ExifInterface.setAttribute(key, null)` expects.
 */
enum class MetadataTag(val key: String) {
    // Location (every key starts with "GPS" — LocationOnly relies on this).
    GpsVersionId("GPSVersionID"),
    GpsLatitude("GPSLatitude"),
    GpsLatitudeRef("GPSLatitudeRef"),
    GpsLongitude("GPSLongitude"),
    GpsLongitudeRef("GPSLongitudeRef"),
    GpsAltitude("GPSAltitude"),
    GpsAltitudeRef("GPSAltitudeRef"),
    GpsTimestamp("GPSTimeStamp"),
    GpsDatestamp("GPSDateStamp"),
    GpsSpeed("GPSSpeed"),
    GpsSpeedRef("GPSSpeedRef"),
    GpsImgDirection("GPSImgDirection"),
    GpsImgDirectionRef("GPSImgDirectionRef"),
    GpsDestBearing("GPSDestBearing"),
    GpsDestBearingRef("GPSDestBearingRef"),
    GpsTrack("GPSTrack"),
    GpsTrackRef("GPSTrackRef"),
    GpsStatus("GPSStatus"),
    GpsMeasureMode("GPSMeasureMode"),
    GpsDop("GPSDOP"),
    GpsSatellites("GPSSatellites"),
    GpsMapDatum("GPSMapDatum"),
    GpsProcessingMethod("GPSProcessingMethod"),
    GpsAreaInformation("GPSAreaInformation"),

    // Dates and times.
    Datetime("DateTime"),
    DatetimeOriginal("DateTimeOriginal"),
    DatetimeDigitized("DateTimeDigitized"),
    OffsetTime("OffsetTime"),
    OffsetTimeOriginal("OffsetTimeOriginal"),
    OffsetTimeDigitized("OffsetTimeDigitized"),
    SubsecTime("SubSecTime"),
    SubsecTimeOriginal("SubSecTimeOriginal"),
    SubsecTimeDigitized("SubSecTimeDigitized"),

    // Identity, device, and authorship.
    ImageDescription("ImageDescription"),
    Make("Make"),
    Model("Model"),
    Software("Software"),
    Artist("Artist"),
    Copyright("Copyright"),
    MakerNote("MakerNote"),
    UserComment("UserComment"),
    ImageUniqueId("ImageUniqueID"),
    CameraOwnerName("CameraOwnerName"),
    BodySerialNumber("BodySerialNumber"),
    LensMake("LensMake"),
    LensModel("LensModel"),
    LensSerialNumber("LensSerialNumber"),
    LensSpecification("LensSpecification"),
    DeviceSettingDescription("DeviceSettingDescription"),
    SubjectLocation("SubjectLocation"),
    SubjectArea("SubjectArea"),

    // Capture parameters.
    ExposureTime("ExposureTime"),
    FNumber("FNumber"),
    PhotographicSensitivity("PhotographicSensitivity"),
    IsoSpeedRatings("ISOSpeedRatings"),
    ShutterSpeedValue("ShutterSpeedValue"),
    ApertureValue("ApertureValue"),
    BrightnessValue("BrightnessValue"),
    ExposureBiasValue("ExposureBiasValue"),
    MaxApertureValue("MaxApertureValue"),
    MeteringMode("MeteringMode"),
    Flash("Flash"),
    FocalLength("FocalLength"),
    FocalLengthIn35mmFilm("FocalLengthIn35mmFilm"),
    WhiteBalance("WhiteBalance"),
    ExposureMode("ExposureMode"),
    ExposureProgram("ExposureProgram"),
    DigitalZoomRatio("DigitalZoomRatio"),
    SceneCaptureType("SceneCaptureType"),
    SensingMethod("SensingMethod"),
    SubjectDistance("SubjectDistance"),
    SubjectDistanceRange("SubjectDistanceRange"),
    ;

    companion object {
        val dateEntries: List<MetadataTag> by lazy {
            listOf(
                Datetime,
                DatetimeOriginal,
                DatetimeDigitized,
                OffsetTime,
                OffsetTimeOriginal,
                OffsetTimeDigitized,
                SubsecTime,
                SubsecTimeOriginal,
                SubsecTimeDigitized,
                GpsTimestamp,
                GpsDatestamp,
            )
        }
    }
}
