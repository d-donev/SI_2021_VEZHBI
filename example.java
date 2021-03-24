package mk.ukim.finki.exams.june.task_management;



import java.io.*;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

interface ITask {
    LocalDateTime getDeadline();

    Integer getPriority();

    String toString();

    default long getTimeLeft() {
        return Math.abs(Duration.between(LocalDateTime.now(), getDeadline()).getSeconds());
    }
}

class SimpleTask implements ITask {

    String name, description;

    public SimpleTask(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public LocalDateTime getDeadline() {
        return LocalDateTime.MAX;
    }

    @Override
    public Integer getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Task{");
        sb.append("name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

abstract class TaskDecorator implements ITask {
    ITask wrappedTask;

    public TaskDecorator(ITask wrappedTask) {
        this.wrappedTask = wrappedTask;
    }
}

class DeadlineDecorator extends TaskDecorator {

    LocalDateTime deadline;

    public DeadlineDecorator(ITask wrappedTask, LocalDateTime deadline) {
        super(wrappedTask);
        this.deadline = deadline;
    }

    @Override
    public LocalDateTime getDeadline() {
        return deadline;
    }

    @Override
    public Integer getPriority() {
        return wrappedTask.getPriority();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(wrappedTask.toString(), 0, wrappedTask.toString().length() - 1);
        sb.append(", deadline=").append(deadline);
        sb.append('}');
        return sb.toString();
    }
}

class PriorityDecorator extends TaskDecorator {

    Integer priority;

    public PriorityDecorator(ITask wrappedTask, Integer priority) {
        super(wrappedTask);
        this.priority = priority;
    }

    @Override
    public LocalDateTime getDeadline() {
        return wrappedTask.getDeadline();
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(wrappedTask.toString(), 0, wrappedTask.toString().length() - 1);
        sb.append(", priority=").append(priority);
        sb.append('}');
        return sb.toString();
    }
}

class DeadlineNotValidException extends Exception {
    public DeadlineNotValidException(LocalDateTime deadline) {
        super(String.format("The deadline %s has already passed", deadline));
    }
}

class TaskCreator {
    public static ITask createTask(String line) throws DeadlineNotValidException {
        String[] parts = line.split(",");
        String name = parts[1];
        String description = parts[2];

        ITask simpleTask = new SimpleTask(name, description);

        if (parts.length == 3)
            return simpleTask;
        if (parts.length == 4) {
            try {
                Integer priority = Integer.parseInt(parts[3]);
                return new PriorityDecorator(simpleTask, priority);
            } catch (Exception e) {
                LocalDateTime deadline = LocalDateTime.parse(parts[3]);
                checkDeadline(deadline);
                return new DeadlineDecorator(simpleTask, deadline);
            }
        } else {
            LocalDateTime deadline = LocalDateTime.parse(parts[3]);
            checkDeadline(deadline);
            Integer priority = Integer.parseInt(parts[4]);
            return new PriorityDecorator(new DeadlineDecorator(simpleTask, deadline), priority);
        }
    }

    private static void checkDeadline(LocalDateTime deadline) throws DeadlineNotValidException {
        if (deadline.isBefore(LocalDateTime.now()))
            throw new DeadlineNotValidException(deadline);
    }

    public static String getCategory(String line) {
        return line.split(",")[0];
    }
}

class TaskManager {

    Map<String, List<ITask>> tasksMap;

    TaskManager() {
        tasksMap = new TreeMap<>();
    }

    public void readTasks(InputStream in) {
        new BufferedReader(new InputStreamReader(in))
                .lines()
                .forEach(line -> {
                    String category = TaskCreator.getCategory(line);
                    try {
                        ITask task = TaskCreator.createTask(line);
                        tasksMap.putIfAbsent(category, new ArrayList<>());
                        tasksMap.computeIfPresent(category, (k, v) -> {
                                    v.add(task);
                                    return v;
                                }
                        );
                    } catch (DeadlineNotValidException e) {
                        System.out.println(e.getMessage());
                    }
                });
    }

    public void printTasks(OutputStream out, boolean includePriority, boolean includeCategory) {
        PrintWriter pw = new PrintWriter(out);

        Comparator<ITask> deadlineComparator = Comparator.comparing(ITask::getTimeLeft);
        Comparator<ITask> priorityAndDeadlineComparator = Comparator.comparing(ITask::getPriority).thenComparing(deadlineComparator);

        Comparator<ITask> taskComparator = includePriority ? priorityAndDeadlineComparator : deadlineComparator;
        if (includeCategory) {
            tasksMap.forEach((category, tasks) -> {
                pw.println(category.toUpperCase());
                tasks.stream().sorted(taskComparator).forEach(pw::println);
            });
        }
        else {
            tasksMap.values().stream()
                    .flatMap(Collection::stream)
                    .sorted(taskComparator)
                    .forEach(pw::println);
        }


        pw.flush();
    }
}

public class TasksManagerTest {

    public static void main(String[] args) {

        TaskManager manager = new TaskManager();

        System.out.println("Tasks reading");
        manager.readTasks(System.in);
        System.out.println("By categories with priority");
        manager.printTasks(System.out, true, true);
        System.out.println("-------------------------");
        System.out.println("By categories without priority");
        manager.printTasks(System.out, false, true);
        System.out.println("-------------------------");
        System.out.println("All tasks without priority");
        manager.printTasks(System.out, false, false);
        System.out.println("-------------------------");
        System.out.println("All tasks with priority");
        manager.printTasks(System.out, true, false);
        System.out.println("-------------------------");

    }
}

