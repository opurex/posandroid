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

import android.app.AlertDialog;
import android.content.DialogInterface;
import com.opurex.client.R;
import com.opurex.client.activities.TrackedActivity;

public class Error {

    /** Alias for showError with a string id and no details. */
    public static void showError(int message, TrackedActivity ctx) {
        Error.showError(ctx.getResources().getString(message), null, ctx);
    }
    /** Alias for showError with a string id. */
    public static void showError(int message, String details,
            TrackedActivity ctx) {
        Error.showError(ctx.getResources().getString(message), details, ctx);
    }
    /** Alias for showError with no details. */
    public static void showError(String message, TrackedActivity ctx) {
        Error.showError(message, null, ctx);
    }
    /** Show an eror popup.
     * @param message The message to display.
     * @param details The technical details of the error (can be null).
     * @param ctx The Activity context. */
    public static void showError(String message, String details,
            TrackedActivity ctx) {
        String content = message;
        if (details != null) {
            content += "\n\nReason: " + details;
        }
        if (ctx.isFront()) {
            AlertDialog.Builder b = new AlertDialog.Builder(ctx);
            b.setTitle(R.string.error_title);
            b.setMessage(content);
            b.setIcon(android.R.drawable.ic_dialog_alert);
            b.setCancelable(true);
            b.setNegativeButton(android.R.string.ok, new DismissListener());
            b.show();
        } else {
            ctx.setPendingError(content);
        }
    }

    private static class DismissListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    }
}
