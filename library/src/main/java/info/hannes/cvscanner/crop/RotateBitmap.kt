/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.hannes.cvscanner.crop

import android.graphics.Bitmap
import android.graphics.Matrix

class RotateBitmap(var bitmap: Bitmap?, initRotation: Int = 0) {

    var rotation: Int

    init {
        if (initRotation == 0)
            rotation = 0
        else
            this.rotation = initRotation % 360
    }

    // We want to do the rotation at origin, but since the bounding
    // rectangle will be changed after rotation, so the delta values
    // are based on old & new width/height respectively.

    // By default this is an identity matrix.
    val rotationMatrix: Matrix
        get() {
            // By default this is an identity matrix.
            val matrix = Matrix()
            if (rotation != 0) {
                // We want to do the rotation at origin, but since the bounding
                // rectangle will be changed after rotation, so the delta values
                // are based on old & new width/height respectively.
                bitmap?.let {
                    val cx = it.width / 2
                    val cy = it.height / 2
                    matrix.preTranslate(-cx.toFloat(), -cy.toFloat())
                    matrix.postRotate(rotation.toFloat())
                    matrix.postTranslate(width / 2.toFloat(), height / 2.toFloat())
                }
            }
            return matrix
        }

    private val isOrientationChanged: Boolean
        get() = rotation / 90 % 2 != 0

    val height: Int
        get() = if (isOrientationChanged)
            bitmap?.width ?: 0
        else
            bitmap?.height ?: 0

    val width: Int
        get() = if (isOrientationChanged)
            bitmap?.height ?: 0
        else
            bitmap?.width ?: 0
}
