/*
    Opurex Android client
    Copyright (C) Opurex contributors, see the COPYRIGHT file

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.opurex.client.utils;

import android.content.Context;

public class ScreenUtils {

    private static final int DENSITY_DPI = 160; // for density 1, xx dpi

    private static float density;
    private static float dpi;

    private static void init(Context ctx) {
        if (ScreenUtils.dpi < 1.0) {
            // Init density
            ScreenUtils.density = ctx.getResources().getDisplayMetrics().density;
            ScreenUtils.dpi = density * ScreenUtils.DENSITY_DPI;
        }
    } 

    /** Convert size in inches to px */
    public static int inToPx(float inches, Context ctx) {
        ScreenUtils.init(ctx);
        return Math.round(inches * ScreenUtils.dpi);
    }
    public static int inToPx(double inches, Context ctx) {
        return ScreenUtils.inToPx((float) inches, ctx);
    }

    public static int dipToPx(float dip, Context ctx) {
        ScreenUtils.init(ctx);
        return Math.round(ScreenUtils.density * dip);
    }
}