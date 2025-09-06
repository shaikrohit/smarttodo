package com.example.smarttodo.utils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 \r2\u00020\u0001:\u0001\rB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0002J\u0018\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u000bH\u0016J\u0018\u0010\f\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0002\u00a8\u0006\u000e"}, d2 = {"Lcom/example/smarttodo/utils/NotificationActionReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "completeTask", "", "context", "Landroid/content/Context;", "taskId", "", "onReceive", "intent", "Landroid/content/Intent;", "snoozeTask", "Companion", "app_debug"})
public final class NotificationActionReceiver extends android.content.BroadcastReceiver {
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_COMPLETE = "com.example.smarttodo.ACTION_COMPLETE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_SNOOZE = "com.example.smarttodo.ACTION_SNOOZE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_TASK_ID = "extra_task_id";
    @org.jetbrains.annotations.NotNull()
    public static final com.example.smarttodo.utils.NotificationActionReceiver.Companion Companion = null;
    
    public NotificationActionReceiver() {
        super();
    }
    
    @java.lang.Override()
    public void onReceive(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
    }
    
    private final void completeTask(android.content.Context context, int taskId) {
    }
    
    private final void snoozeTask(android.content.Context context, int taskId) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/example/smarttodo/utils/NotificationActionReceiver$Companion;", "", "()V", "ACTION_COMPLETE", "", "ACTION_SNOOZE", "EXTRA_TASK_ID", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}