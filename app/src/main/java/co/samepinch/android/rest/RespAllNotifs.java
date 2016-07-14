package co.samepinch.android.rest;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by imaginationcoder on 7/1/15.
 */
public class RespAllNotifs extends RestBase<RespAllNotifs.Body> {

    @Override
    public Body getBody() {
        return super.getBody();
    }

    @Override
    public void setBody(Body body) {
        super.setBody(body);
    }

    public static class Body {
        @SerializedName("count")
        private String count;

        @SerializedName("notification_count")
        private String notifsCount;

        @SerializedName("last_modified")
        private String lastModified;

        private String etag;

        List<Notification> notifications;

        public Body() {
        }

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }

        public String getNotifsCount() {
            return notifsCount;
        }

        public void setNotifsCount(String notifsCount) {
            this.notifsCount = notifsCount;
        }

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public String getEtag() {
            return etag;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }

        public List<Notification> getNotifications() {
            return notifications;
        }

        public void setNotifications(List<Notification> notifications) {
            this.notifications = notifications;
        }
    }

    public static class Notification {
        private String uid;
        private String message;
        private String source;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        private String sourceId;
        private String viewed;


        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getViewed() {
            return viewed;
        }

        public void setViewed(String viewed) {
            this.viewed = viewed;
        }
    }
}