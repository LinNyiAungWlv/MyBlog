package wlv.ac.uk.myblog.model;

/**
 * A single blog message stored on the device.
 * Each field matches a column in the "messages" table (see DbHelper).
 */
public class Message {

    private long id;            // database id, 0 means "not saved yet"
    private String title;
    private String body;
    private String imagePath;   // path to the attached image file, or null if no image
    private long createdAt;     // time created, in milliseconds
    private long updatedAt;     // time last edited, in milliseconds
    private boolean uploaded;   // true once the message has been posted online

    /** Empty constructor, used when building a message field by field. */
    public Message() {
    }

    /** Constructor for a new message that has not been saved yet. */
    public Message(String title, String body, String imagePath) {
        this.title = title;
        this.body = body;
        this.imagePath = imagePath;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    /** Returns true if this message has an image attached. */
    public boolean hasImage() {
        return imagePath != null && !imagePath.isEmpty();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }
}
