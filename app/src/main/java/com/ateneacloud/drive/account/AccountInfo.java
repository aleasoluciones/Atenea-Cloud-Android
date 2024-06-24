package com.ateneacloud.drive.account;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.ateneacloud.drive.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class used to manage Account information
 */
public class AccountInfo implements Parcelable, Comparable<AccountInfo> {
    private static final String DEBUG_TAG = "AccountInfo";

    public static final String SPACE_USAGE_SEPERATOR = " / ";
    private long usage;
    private long total;
    private String email;
    private String server;
    private String name;
    private Enum plan;

    private AccountInfo() {
    }

    protected AccountInfo(Parcel in) {
        usage = in.readLong();
        total = in.readLong();
        email = in.readString();
        server = in.readString();
        name = in.readString();
        plan = AccountPlans.valueOf(in.readString());
    }

    public static AccountInfo fromJson(JSONObject accountInfo, String server) throws JSONException {
        AccountInfo info = new AccountInfo();
        info.server = server;
        info.usage = accountInfo.getLong("usage");
        info.total = accountInfo.getLong("total");
        info.email = accountInfo.getString("email");
        info.name = accountInfo.getString("name");

        try {
            long planInGB = (info.total / (1000L * 1000L * 1000L));
            if (planInGB == 100) {
                info.plan = AccountPlans.Basic;
            } else if (planInGB == 200) {
                info.plan = AccountPlans.Standard;
            } else if (planInGB == 500) {
                info.plan = AccountPlans.Enterprise;
            } else if (planInGB == 2000) {
                info.plan = AccountPlans.Platinum;
            } else {
                info.plan = AccountPlans.Basic;
            }
        } catch (Exception e) {
            e.printStackTrace();
            info.plan = AccountPlans.Basic;
        }

        //info.plan = AccountPlans.Platinum;

        return info;
    }

    public long getUsage() {
        return usage;
    }

    public long getTotal() {
        return total;
    }

    public String getEmail() {
        return email;
    }

    public String getServer() {
        return server;
    }

    public String getName() {
        return name;
    }

    public String getSpaceUsed() {
        String strUsage = Utils.readableFileSize(usage);
        String strTotal = Utils.readableFileSize(total);
        return strUsage + SPACE_USAGE_SEPERATOR + strTotal;
    }

    public String getPlanFormat() {
        return plan.toString();
    }

    public Enum getPlan() {
        return plan;
    }

    public long getSpeedUpload() {

        if (plan == AccountPlans.Enterprise) {
            return 2 * 1024 * 1024;
        } else {
            return 1 * 1024 * 1024;
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(this.usage);
        dest.writeLong(this.total);
        dest.writeString(this.email);
        dest.writeString(this.server);
        dest.writeString(this.name);
        dest.writeString(this.plan.toString());
    }

    @Override
    public int compareTo(AccountInfo o) {
        return this.toString().compareTo(o.toString());
    }

    public static final Creator<AccountInfo> CREATOR = new Creator<AccountInfo>() {
        @Override
        public AccountInfo createFromParcel(Parcel in) {
            return new AccountInfo(in);
        }

        @Override
        public AccountInfo[] newArray(int size) {
            return new AccountInfo[size];
        }
    };
}
