package model;

import androidx.annotation.NonNull;

public class SubTaskModel {
    private String id;
    private String content;
    private boolean isDone;

    public SubTaskModel() {
    }

    public SubTaskModel(String id, String content, boolean isDone) {
        this.id = id;
        this.content = content;
        this.isDone = isDone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean getIsDone() {
        return isDone;
    }

    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }

    @NonNull
    @Override
    public String toString() {
        return "SubTaskModel{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", isDone=" + isDone +
                '}';
    }
}