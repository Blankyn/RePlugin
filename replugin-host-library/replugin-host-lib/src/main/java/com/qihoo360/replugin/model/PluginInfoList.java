/*
 * Copyright (C) 2005-2017 Qihoo 360 Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.qihoo360.replugin.model;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.qihoo360.loader2.Constant;
import com.qihoo360.replugin.RePlugin;
import com.qihoo360.replugin.helper.LogDebug;
import com.qihoo360.replugin.utils.Charsets;
import com.qihoo360.replugin.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author RePlugin Team
 */

public class PluginInfoList implements Iterable<PluginInfo> {

    private static final String TAG = "PluginInfoList";

    private final ConcurrentHashMap<String, PluginInfo> mMap = new ConcurrentHashMap<>();

    public void add(PluginInfo pi) {
        addToMap(pi);
    }

    /**
     * 强制更新内存列表，比如安装后需要立即更新p.l的情况
     * @param pi
     */
    public void addForce(PluginInfo pi) {
        if (pi == null) return;
        if (!TextUtils.isEmpty(pi.getName())) mMap.put(pi.getName(), pi);
        if (!TextUtils.isEmpty(pi.getAlias())) mMap.put(pi.getAlias(), pi);
    }

    public void remove(String pn) {
        mMap.remove(pn);
    }

    public PluginInfo get(String pn) {
        return pn != null ? mMap.get(pn) : null;
    }

    public List<PluginInfo> cloneList() {
        return new ArrayList<>(getCopyValues());
    }

    public boolean load(Context context) {
        try {
            // 1. 读出字符串
            final File f = getFile(context);

            // fix: added by qfmeng
            // java.io.FileNotFoundException:
            // File '/data/data/com.qihoo360.replugin.sample.host/app_p_a/p.l' does not exist
            if (!f.exists()) {
                if (LogDebug.LOG) {
                    LogDebug.e(TAG, "load: file exist");
                }
                return false;
            }

            final String result = FileUtils.readFileToString(f, Charsets.UTF_8);
            if (TextUtils.isEmpty(result)) {
                if (LogDebug.LOG) {
                    LogDebug.e(TAG, "load: Read Json error!");
                }
                return false;
            }

            // 2. 解析出JSON
            final JSONArray jArr = new JSONArray(result);
            for (int i = 0; i < jArr.length(); i++) {
                final JSONObject jo = jArr.optJSONObject(i);
                final PluginInfo pi = PluginInfo.createByJO(jo);
                if (pi == null) {
                    if (LogDebug.LOG) {
                        LogDebug.e(TAG, "load: PluginInfo Invalid. Ignore! jo=" + jo);
                    }
                    continue;
                }

                //block状态的插件丢弃
                if (RePlugin.getConfig().getCallbacks().isPluginBlocked(pi)) {
                    continue;
                }

                addToMap(pi);
            }
            return true;
        } catch (IOException e) {
            if (LogDebug.LOG) {
                LogDebug.e(TAG, "load: Load error!", e);
            }
        } catch (JSONException e) {
            if (LogDebug.LOG) {
                LogDebug.e(TAG, "load: Parse Json Error!", e);
            }
        }
        return false;
    }

    public boolean save(Context context) {
        try {
            final File f = getFile(context);
            final JSONArray jsonArr = new JSONArray();
            for (PluginInfo i : getCopyValues()) jsonArr.put(i.getJSON());
            if (LogDebug.LOG) {
                Log.d(LogDebug.TAG_NO_PN, "save json into p.l=" + jsonArr.toString());
            }
            FileUtils.writeStringToFile(f, jsonArr.toString(), Charsets.UTF_8);
            return true;
        } catch (IOException e) {
            if (LogDebug.LOG) {
                e.printStackTrace();
            }
            return false;
        }
    }

    @Override
    public Iterator<PluginInfo> iterator() {
        return getCopyValues().iterator();
    }

    ///

    private Collection<PluginInfo> getCopyValues() {
        return new HashSet(mMap.values()); //是否有必要去重???
    }

    private void addToMap(PluginInfo pi) {
        if (pi == null) return;
        if (!TextUtils.isEmpty(pi.getName())) updateMap(pi.getName(), pi);
        if (!TextUtils.isEmpty(pi.getAlias())) updateMap(pi.getAlias(), pi);
    }

    private void updateMap(String name, PluginInfo info) {
        if (mMap.contains(info)) {
            return;
        }
        if (LogDebug.LOG) {
            Log.d(TAG, "updateMap=" + info + ",address=" + System.identityHashCode(info));
        }
        //解决其他进程重启后，重新加载，导致内存中的PluginInfo对象被替换的问题
        if (mMap.containsKey(name)) {
            PluginInfo oriInfo = mMap.get(name);
            if (oriInfo != null) {
                oriInfo.updateAll(info);
                mMap.put(name, oriInfo);
            } else {
                mMap.put(name, info);
            }
        } else {
            mMap.put(name, info);
        }
    }

    private File getFile(Context context) {
        final File d = context.getDir(Constant.LOCAL_PLUGIN_APK_SUB_DIR, 0);
        return new File(d, "p.l");
    }
}
