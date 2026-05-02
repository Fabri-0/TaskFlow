package com.taskflow.data.repository;

public interface ResultCallback<T> {
    void onSuccess(T value);

    void onError(String message);
}
