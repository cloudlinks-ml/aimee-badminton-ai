package com.cloudlinks.aimeebadminton.vision

/**
 * Standard badminton court geometry (metres), used as the initial calibration
 * prior. The visible court lines still need to be detected from the camera view;
 * these dimensions are not a substitute for camera calibration.
 */
object BadmintonCourt {
    const val LENGTH_METRES = 13.40
    const val DOUBLES_WIDTH_METRES = 6.10
    const val SINGLES_WIDTH_METRES = 5.18
    const val SHORT_SERVICE_LINE_FROM_NET_METRES = 1.98
    const val DOUBLES_LONG_SERVICE_LINE_FROM_BASELINE_METRES = 0.76
    const val NET_HEIGHT_CENTRE_METRES = 1.524
    const val NET_HEIGHT_POSTS_METRES = 1.55
}
