/*
** Copyright 2015, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package android.view;

import android.app.ActivityManagerNative;
import android.os.IBinder;
import android.os.RemoteException;
import com.android.internal.view.IDropPermissions;
import dalvik.system.CloseGuard;


/**
 * {@link DropPermissions} controls the access permissions for the content URIs associated with a
 * {@link DragEvent}.
 * <p>
 * Permission are granted when this object is created by {@link
 * android.app.Activity#requestDropPermissions(DragEvent) Activity.requestDropPermissions}.
 * Which permissions are granted is defined by the set of flags passed to {@link
 * View#startDragAndDrop(android.content.ClipData, View.DragShadowBuilder, Object, int)
 * View.startDragAndDrop} by the app that started the drag operation.
 * <p>
 * The life cycle of the permissions is bound to the activity used to call {@link
 * android.app.Activity#requestDropPermissions(DragEvent) requestDropPermissions}. The
 * permissions are revoked when this activity is destroyed, or when {@link #release()} is called,
 * whichever occurs first.
 */
public final class DropPermissions {

    private final IDropPermissions mDropPermissions;

    private IBinder mPermissionOwnerToken;

    private final CloseGuard mCloseGuard = CloseGuard.get();

    /**
     * Create a new {@link DropPermissions} object to control the access permissions for content
     * URIs associated with {@link DragEvent}.
     * @param dragEvent Drag event
     * @return {@link DropPermissions} object or null if there are no content URIs associated with
     * the {@link DragEvent}.
     * @hide
     */
    public static DropPermissions obtain(DragEvent dragEvent) {
        if (dragEvent.getDropPermissions() == null) {
            return null;
        }
        return new DropPermissions(dragEvent.getDropPermissions());
    }

    /** @hide */
    private DropPermissions(IDropPermissions dropPermissions) {
        mDropPermissions = dropPermissions;
    }

    /**
     * Take the permissions and bind their lifetime to the activity.
     * @param activityToken Binder pointing to an Activity instance to bind the lifetime to.
     * @return True if permissions are successfully taken.
     * @hide
     */
    public boolean take(IBinder activityToken) {
        try {
            mDropPermissions.take(activityToken);
        } catch (RemoteException e) {
            return false;
        }
        mCloseGuard.open("release");
        return true;
    }

    /**
     * Take the permissions. Must call {@link #release} explicitly.
     * @return True if permissions are successfully taken.
     * @hide
     */
    public boolean takeTransient() {
        try {
            mPermissionOwnerToken = ActivityManagerNative.getDefault().
                    newUriPermissionOwner("drop");
            mDropPermissions.takeTransient(mPermissionOwnerToken);
        } catch (RemoteException e) {
            return false;
        }
        mCloseGuard.open("release");
        return true;
    }

    /**
     * Revoke permissions explicitly.
     */
    public void release() {
        try {
            mDropPermissions.release();
            mPermissionOwnerToken = null;
        } catch (RemoteException e) {
        }
        mCloseGuard.close();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (mCloseGuard != null) {
                mCloseGuard.warnIfOpen();
            }
            release();
        } finally {
            super.finalize();
        }
    }
}
