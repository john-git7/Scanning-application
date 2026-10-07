import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScanController {

    private final List<Scan> queue = new ArrayList<>();
    private Scan currentScan;
    private Thread workerThread;
    private boolean running = false;
    private boolean exiting = false;

    public void handleCommand(String command) {

        if (command == null || command.trim().isEmpty()) {
            System.out.println("Invalid command");
            return;
        }

        command = command.trim();

        if (command.equalsIgnoreCase("view")) {
            view();

        } else if (command.equalsIgnoreCase("start")) {
            start();

        } else if (command.equalsIgnoreCase("stop")) {
            stop();

        } else if (command.equalsIgnoreCase("exit")) {
            exit();

        } else if (command.toLowerCase().startsWith("add:")) {
            addScan(command);

        } else if (command.toLowerCase().startsWith("remove:")) {
            removeScan(command);

        } else {
            System.out.println("Unknown command: " + command);
        }
    }



    private synchronized void addScan(String command) {

        try {
            String data = command.substring(4);
            String[] parts = data.split(",");

            if (parts.length != 4) {
                System.out.println("Invalid add command. Expected: add:<id>, <name>, <duration>, <pause>");
                return;
            }

            int id = Integer.parseInt(parts[0].trim());
            String name = parts[1].trim();
            int duration = Integer.parseInt(parts[2].trim());
            String pauseValue = parts[3].trim();
            boolean pause;

            if (pauseValue.equalsIgnoreCase("yes")) {
                pause = true;
            } else if (pauseValue.equalsIgnoreCase("no")) {
                pause = false;
            } else {
                System.out.println("Pause must be Yes or No");
                return;
            }

            if (duration <= 0) {
                System.out.println("Duration must be greater than 0");
                return;
            }

            for (Scan scan : queue) {
                if (scan.getId() == id) {
                    System.out.println("Scan with ID " + id + " already exists");
                    return;
                }
            }

            Scan scan = new Scan(id,name,duration,pause);
            queue.add(scan);
            System.out.println("Added " + name);

        } catch (NumberFormatException e) {
            System.out.println("ID and duration must be numbers");

        } catch (Exception e) {
            System.out.println("Invalid add command");
        }
    }

    private synchronized void view() {

        if (queue.isEmpty()) {
            System.out.println("Queue is empty");
            return;
        }

        System.out.println("Current Queue:");

        for (Scan scan : queue) {
            System.out.println(scan);
        }
    }

    private synchronized void start() {

        if (exiting) {
            return;
        }

        if (running) {
            System.out.println("Scanning is already in progress");
            return;
        }

        Scan nextScan = getNextIdleScan();

        if (nextScan == null) {
            System.out.println("No scans available to start");
            return;
        }
        running = true;
        workerThread = new Thread(() -> {
            while (true) {
                Scan scan;
                synchronized (ScanController.this) {

                    if (exiting) {
                        running = false;
                        return;
                    }
                    scan = getNextIdleScan();
                    if (scan == null) {
                        running = false;
                        return;
                    }
                    currentScan = scan;
                }
                runScan(scan);
                synchronized (ScanController.this) {
                    currentScan = null;
                    if (scan.getState() == ScanState.CANCELLED) {
                        continue;
                    }

                    if (scan.getState() == ScanState.COMPLETE && scan.isPause()) {
                        running = false;
                        System.out.println("Scanning paused. Type start to continue.");
                        return;
                    }
                }
            }
        });

        workerThread.start();
        System.out.println("Scanning started");
    }

    private synchronized Scan getNextIdleScan() {
        for (Scan scan : queue) {
            if (scan.getState() == ScanState.IDLE) {
                return scan;
            }
        }
        return null;
    }

    private void runScan(Scan scan) {
        synchronized (scan) {
            if (scan.getState() != ScanState.IDLE) {
                return;
            }
            scan.setState(ScanState.RUNNING);
        }

        System.out.println("Starting " + scan.getName());

        try {
            Thread.sleep(scan.getDuration() * 1000L);

            synchronized (scan) {
                if (scan.getState() == ScanState.RUNNING) {
                    scan.setState(ScanState.COMPLETE);
                    System.out.println("Completed " + scan.getName());
                }
            }
        } catch (InterruptedException e) {
            synchronized (scan) {
                if (scan.getState() == ScanState.RUNNING) {
                    scan.setState(ScanState.CANCELLED);
                }
            }
            System.out.println("Cancelled " + scan.getName());
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void stop() {
        if (currentScan == null) {
            System.out.println("No scan is currently running");
            return;
        }

        Scan scan = currentScan;
        synchronized (scan) {
            if (scan.getState()!= ScanState.RUNNING) {
                System.out.println("No scan is currently running");
                return;
            }
            scan.setState(ScanState.CANCELLED);
        }

        System.out.println("Stopping " + scan.getName());

        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private synchronized void removeScan(String command) {
        try {
            String idText = command.substring(7).trim();
            int id = Integer.parseInt(idText);
            Iterator<Scan> iterator = queue.iterator();

            while (iterator.hasNext()) {
                Scan scan = iterator.next();
                if (scan.getId() == id) {
                    if (scan.getState() != ScanState.IDLE) {
                        System.out.println("Cannot remove scan " + id + " because it has already started");
                        return;
                    }
                    iterator.remove();
                    System.out.println("Removed " + scan.getName());
                    return;
                }
            }
            System.out.println("Scan " + id + " not found");
        } catch (NumberFormatException e) {
            System.out.println("Invalid scan ID");

        } catch (Exception e) {
            System.out.println("Invalid remove command");
        }
    }

    private synchronized void exit() {
        exiting = true;
        if (currentScan != null) {
            synchronized (currentScan) {
                if (currentScan.getState() == ScanState.RUNNING) {
                    currentScan.setState(ScanState.CANCELLED);
                }
            }
        }

        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }
        System.out.println("Exiting application...");
    }
}