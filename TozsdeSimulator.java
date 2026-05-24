import java.io.*;
import java.util.*;

public class TozsdeSimulator {
    static class Ceg {
        String nev;
        double ar;
        int sajatDb;
        String hir;
        double trendMeroseg;

        Ceg(String nev, double induloAr) {
            this.nev = nev;
            this.ar = induloAr;
            this.sajatDb = 0;
            this.hir = "A cég stabil.";
            this.trendMeroseg = 0;
        }
    }

    private static final double ALAP_EGYENLEG = 10000.0;
    private static double egyenleg = ALAP_EGYENLEG;
    private static final Ceg[] cegek = {
            new Ceg("Nvidia", 450.0),
            new Ceg("AMD", 160.0),
            new Ceg("Intel", 35.0),
            new Ceg("Microsoft", 420.0),
            new Ceg("Apple", 180.0)
    };

    private static final String SAVE_FILE = "user_data.dat";
    private static volatile boolean fut = true;
    private static String rendszerUzenet = "Nyomj egy Entert a frissítéshez vagy adj meg parancsot!";

    public static void main(String[] args) {
        betoltAdatok();

        Thread tozdeMotor = new Thread(() -> {
            Random random = new Random();
            while (fut) {
                try {
                    for (Ceg ceg : cegek) {
                        double valtozas = (random.nextDouble() - 0.5) * (ceg.ar * 0.03) + ceg.trendMeroseg;
                        ceg.ar = Math.max(5.0, ceg.ar + valtozas);

                        if (random.nextInt(20) > 17) {
                            generalCegHir(ceg, random);
                        }
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        tozdeMotor.start();

        Scanner sc = new Scanner(System.in);
        while (fut) {
            clearConsole();
            printUI();

            String parancs = sc.nextLine().toLowerCase().trim();

            switch (parancs) {
                case "vetel" -> tranzakcio(sc, true);
                case "eladas" -> tranzakcio(sc, false);
                case "tart" -> rendszerUzenet = "[INFO] Pozíciók tartva. Figyeled a piacot...";
                case "reset" -> ujJatek();
                case "kilep" -> {
                    mentesAdatok();
                    fut = false;
                    tozdeMotor.interrupt();
                }
                default -> rendszerUzenet = "[HIBA] Ismeretlen parancs! (vetel/eladas/tart/reset/kilep)";
            }
        }
        System.out.println("Viszlát a legközelebbi nyitásig!");
    }

    private static void printUI() {
        System.out.println("===============================================================================");
        System.out.println("                         TECH TOZSDE SIMULATOR 2026                            ");
        System.out.println("===============================================================================");
        System.out.printf(" AKTUÁLIS EGYENLEGED: %.2f EUR\n", egyenleg);
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf(" %-12s | %-12s | %-10s | %s\n", "CÉG NEVE", "ÁRFOLYAM", "PORTFÓLIÓ", "LEGFRISSEBB HÍR");
        System.out.println("-------------------------------------------------------------------------------");
        for (Ceg ceg : cegek) {
            System.out.printf(" %-12s | %-9.2f EUR | %-7d db | %s\n",
                    ceg.nev, ceg.ar, ceg.sajatDb, ceg.hir);
        }
        System.out.println("===============================================================================");
        System.out.println(" STATUSZ: " + rendszerUzenet);
        System.out.print(" Parancsod (vetel/eladas/tart/reset/kilep): ");
    }

    private static void generalCegHir(Ceg ceg, Random r) {
        String[] pozitiv = {"Új AI chipet jelentettek be!", "A negyedéves jelentés felülmúlta a várakozásokat.", "Rekordmennyiségű előrendelés."};
        String[] negativ = {"Ellátási lánc problémák adódtak.", "A trösztellenes vizsgálat rontja a kilátásokat.", "Csökkenő eladási számok."};

        if (r.nextBoolean()) {
            ceg.hir = "▲ " + pozitiv[r.nextInt(pozitiv.length)];
            ceg.trendMeroseg = ceg.ar * 0.015;
        } else {
            ceg.hir = "▼ " + negativ[r.nextInt(negativ.length)];
            ceg.trendMeroseg = -(ceg.ar * 0.015);
        }
    }

    private static void tranzakcio(Scanner sc, boolean vetel) {
        if (!vetel) {
            int osszesReszveny = 0;
            for (Ceg c : cegek) osszesReszveny += c.sajatDb;
            if (osszesReszveny <= 0) {
                rendszerUzenet = "[HIBA] Nincs semmilyen részvényed, amit eladhatnál!";
                return;
            }
        }

        System.out.print("Melyik cég? (Nvidia/AMD/Intel/Microsoft/Apple): ");
        String valasztottNev = sc.nextLine().trim();
        Ceg valasztottCeg = null;

        for (Ceg c : cegek) {
            if (c.nev.equalsIgnoreCase(valasztottNev)) {
                valasztottCeg = c;
                break;
            }
        }

        if (valasztottCeg == null) {
            rendszerUzenet = "[HIBA] Nem létezik ilyen cég a listában!";
            return;
        }

        if (!vetel && valasztottCeg.sajatDb <= 0) {
            rendszerUzenet = "[HIBA] Nincs részvényed a(z) " + valasztottCeg.nev + " cégben!";
            return;
        }

        try {
            System.out.print("Mennyiség: ");
            int db = Integer.parseInt(sc.nextLine());
            if (db <= 0) throw new NumberFormatException();

            if (vetel) {
                double koltseg = db * valasztottCeg.ar;
                if (koltseg <= egyenleg) {
                    egyenleg -= koltseg;
                    valasztottCeg.sajatDb += db;
                    rendszerUzenet = "[SIKER] Vettél " + db + " db " + valasztottCeg.nev + " részvényt!";
                } else {
                    rendszerUzenet = "[SIKERTELEN] Nincs elég pénzed erre a mennyiségre!";
                }
            } else {
                if (db <= valasztottCeg.sajatDb) {
                    egyenleg += db * valasztottCeg.ar;
                    valasztottCeg.sajatDb -= db;
                    rendszerUzenet = "[SIKER] Eladtál " + db + " db " + valasztottCeg.nev + " részvényt!";
                } else {
                    rendszerUzenet = "[SIKERTELEN] Nincs ennyi részvényed ebből! (Birtokolt: " + valasztottCeg.sajatDb + " db)";
                }
            }
        } catch (Exception e) {
            rendszerUzenet = "[HIBA] Érvénytelen darabszám!";
        }
    }

    private static void ujJatek() {
        File f = new File(SAVE_FILE);
        if (f.exists()) f.delete();
        egyenleg = ALAP_EGYENLEG;

        cegek[0] = new Ceg("Nvidia", 200.0);
        cegek[1] = new Ceg("AMD", 470.0);
        cegek[2] = new Ceg("Intel", 120.0);
        cegek[3] = new Ceg("Microsoft", 420.0);
        cegek[4] = new Ceg("Apple", 310.0);

        rendszerUzenet = "[RESET] Minden adat és mentés törölve. Kezdődhet az új futam!";
    }

    private static void mentesAdatok() {
        try (RandomAccessFile raf = new RandomAccessFile(SAVE_FILE, "rw")) {
            raf.seek(0);
            raf.writeDouble(egyenleg);

            for (Ceg ceg : cegek) {
                raf.writeInt(ceg.sajatDb);
            }
        } catch (IOException ignored) {}
    }

    private static void betoltAdatok() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;
        try (RandomAccessFile raf = new RandomAccessFile(SAVE_FILE, "r")) {
            egyenleg = raf.readDouble();
            for (Ceg ceg : cegek) {
                ceg.sajatDb = raf.readInt();
            }
        } catch (IOException e) {
            rendszerUzenet = "[HIBA] Nem sikerült az adatok betöltése!";
        }
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }
}