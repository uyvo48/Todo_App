package model;

import androidx.annotation.NonNull;

public class TaskModel {
    private String id; // Thuộc tính id để lưu taskId
    private String title;

    public TaskModel() {
    }

    public TaskModel(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @NonNull
    @Override
    public String toString() {
        return "TaskModel{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}