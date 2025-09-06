package com.example.smarttodo;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002R\u001b\u0010\u0003\u001a\u00020\u00048FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0007\u0010\b\u001a\u0004\b\u0005\u0010\u0006R\u001b\u0010\t\u001a\u00020\n8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\b\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u000e"}, d2 = {"Lcom/example/smarttodo/SmartTodoApplication;", "Landroid/app/Application;", "()V", "database", "Lcom/example/smarttodo/data/TaskDatabase;", "getDatabase", "()Lcom/example/smarttodo/data/TaskDatabase;", "database$delegate", "Lkotlin/Lazy;", "repository", "Lcom/example/smarttodo/data/TaskRepository;", "getRepository", "()Lcom/example/smarttodo/data/TaskRepository;", "repository$delegate", "app_debug"})
public final class SmartTodoApplication extends android.app.Application {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy database$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy repository$delegate = null;
    
    public SmartTodoApplication() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.smarttodo.data.TaskDatabase getDatabase() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.example.smarttodo.data.TaskRepository getRepository() {
        return null;
    }
}