/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ban.file.repair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.*;

/**
 * @author manuj
 */
public class HelperClass {

    public static String SEQUENCE_HEADER = "% SEQUENCE_COMPLETE";
    public static String DELETE_HEADER = "% DELETION_COMPLETE";

    public static ArrayList<File> getBANFiles(String directory) {
        File folder = new File(directory);
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> banFiles = new ArrayList();
        for (int i = 0; i < listOfFiles.length; i++)
            if (listOfFiles[i].getName().endsWith("ban"))
                banFiles.add(listOfFiles[i]);

        return banFiles;
    }

    public static void sequenceEachFile(ArrayList<File> banFiles, int sequencerMode) {
        for (int i = 0; i < banFiles.size(); i++) {
            System.out.println(banFiles.get(i).getName());
            ArrayList<String> fileLines = new ArrayList();
            ArrayList<String> finalData = new ArrayList();
            File file = banFiles.get(i);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();
                }
                reader.close();

                if (headerCheck(fileLines, SEQUENCE_HEADER))
                    continue;
                else
                    MainUI.filesOperated.add(banFiles.get(i));

                finalData = sequenceAndSort(fileLines, sequencerMode);

                overriteFile(finalData, banFiles.get(i));

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (fileLines != null)
                fileLines.clear();

        }
    }

    private static boolean headerCheck(ArrayList<String> fileLines, String sequenceHeader) {
        for (int j = 0; j < 10; j++)
            if (fileLines.get(j).contains(sequenceHeader))
                return true;

        return false;
    }

    public static ArrayList<String> sequenceAndSort(ArrayList<String> fileLines, int sequencerMode) {
        ArrayList<String> sequencingData = new ArrayList();
        ArrayList<String> dataWithoutSequence = new ArrayList();
        ArrayList<Double> yValues = new ArrayList();
        ArrayList<String> finalData = new ArrayList();


        int Z0Entry = 0, Z0Middle = 0, Z0Exit = 0;
        int FinalZ0 = 0;
        String First, Second, Third;

        finalData.add(SEQUENCE_HEADER);

        //Determing Entry and exit points
        for (int i = 0; i < fileLines.size(); i++) {
            String f = fileLines.get(i);
            if (f.contains("Z0")) {
                Z0Entry = i;
                break;
            }
        }

        for (int i = Z0Entry; i < fileLines.size() - 2; i++) {
            String f = fileLines.get(i);
            String f1, f2;
            if (fileLines.get(i + 1) != null && fileLines.get(i + 2) != null) {
                f1 = fileLines.get(i + 1);
                f2 = fileLines.get(i + 2);
            } else
                f1 = f2 = "DUMMY";

            if (f.startsWith("Z0") && f1.startsWith("X") && f2.startsWith("Z"))
                Z0Middle = i;
        }

        for (int i = fileLines.size() - 1; i > 0; i--) {
            String f = fileLines.get(i);
            if (f.contains("Z0")) {
                Z0Exit = i;
                break;
            }

        }


        for (int i = 0; i < fileLines.size(); i++) {
            if (i >= Z0Entry && i <= Z0Middle)
                sequencingData.add(fileLines.get(i));
            else if ((i == Z0Middle + 1) || (i == Z0Middle + 2))
                sequencingData.add(fileLines.get(i));
            else
                dataWithoutSequence.add(fileLines.get(i));
        }

        for (int i = 0; i < sequencingData.size(); i++) {
            String f = sequencingData.get(i);
            Double data;
            if (f.contains("Y")) {
                data = Double.parseDouble(f.substring(f.indexOf('Y') + 1, f.length() - 1));
                yValues.add(data);
            }
        }

        if (sequencerMode == 1)
            Collections.sort(yValues);
        else
            Collections.sort(yValues, Collections.reverseOrder());


        for (int i = 0; i < dataWithoutSequence.size(); i++)
            if (dataWithoutSequence.get(i).contains("Z0"))
                FinalZ0 = i;


        for (int i = 0; i < FinalZ0; i++)
            finalData.add(dataWithoutSequence.get(i));

        First = sequencingData.get(0);
        Third = sequencingData.get(2);

        for (int i = 0; i < yValues.size(); i++) {
            String xyValues = "DUMMY";
            finalData.add("\n");
            finalData.add(First);

            String Y = String.valueOf(yValues.get(i));
            for (int j = 0; j < sequencingData.size(); j++) {
                if (sequencingData.get(j).startsWith("X") && sequencingData.get(j).contains(Y)) {
                    finalData.add(sequencingData.get(j));
                    xyValues = sequencingData.get(j);
                    break;
                }
            }


            finalData.add(Third);

            //new duplicate pair with z0 as the third line
            finalData.add("\n");
            finalData.add(First);
            if (!xyValues.equals("DUMMY"))
                finalData.add(xyValues);
            finalData.add("Z0.000000");
            finalData.add("\n");


        }


        finalData.add("\n");

        for (int i = 0; i < sequencingData.size(); i++)
            System.out.println(sequencingData.get(i));

        for (int i = FinalZ0; i < dataWithoutSequence.size(); i++) {
            finalData.add(dataWithoutSequence.get(i));
        }


        finalData = addM0AfterM3(finalData);


        return finalData;

    }

    // This function is part of sequence and sort and adds an M0 just after M3
    public static ArrayList<String> addM0AfterM3(ArrayList<String> almostFinalData) {
        String mTHREE = "M3";
        String mZERO = "M0";
        ArrayList<String> finalData = new ArrayList<>();
        boolean flag = false;

        for (int i = 0; i < almostFinalData.size(); i++) {

            finalData.add(almostFinalData.get(i));
            if (almostFinalData.get(i).equals(mTHREE)) {
                if (!flag) {
                    finalData.add("\n"); // adding extra line before M0
                    finalData.add(mZERO);
                    flag = true;
                }
            }
        }

        return finalData;

    }

    public static void overriteFile(ArrayList<String> finalData, File banFile) throws IOException {
        Writer writer = new FileWriter(banFile.getAbsolutePath(), false);

        for (int i = 0; i < finalData.size(); i++) {
            writer.write(finalData.get(i));
            writer.write(System.lineSeparator());
        }
        writer.close();
    }

    public static void DeleteMode(ArrayList<File> banFiles) {
        for (int i = 0; i < banFiles.size(); i++) {
            System.out.println(banFiles.get(i).getName());
            ArrayList<String> fileLines = new ArrayList();
            File file = banFiles.get(i);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();
                }
                reader.close();

                if (headerCheck(fileLines, DELETE_HEADER))
                    continue;
                else
                    MainUI.filesOperated.add(banFiles.get(i));

                getDeletedData(fileLines, file);

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fileLines != null)
                fileLines.clear();
        }

    }


    public static void getDeletedData(ArrayList<String> fileLines, File file) {
        ArrayList<Integer> steps = new ArrayList<>();
        ArrayList<String> finalData = new ArrayList<>();
        int step = 0;
        finalData.add(DELETE_HEADER);

        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            if (line.contains("LLLLL")) {
                step = i;
                for (int j = 0; j < 3; j++)
                    finalData.remove(finalData.size() - 1);
            } else if (step > 1 && i == (step + 1))
                finalData.add("% LLL");
            else
                finalData.add(line);

        }

        try {
            overriteFile(finalData, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* deletePartMode - it does not delete stuff anymore
       functionality was changed in VER_5.3
     */
    public static void deletePartMode(ArrayList<File> banFiles) {
        for (int i = 0; i < banFiles.size(); i++) {
            System.out.println(banFiles.get(i).getName());
            ArrayList<String> fileLines = new ArrayList();
            ArrayList<Integer> pSteps = new ArrayList<>();
            File file = banFiles.get(i);
            BufferedReader reader;
            int pCount = 0;
            try {
                reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();
                }
                reader.close();

                pSteps.add(0);
                for (int j = 0; j < fileLines.size(); j++) {
                    if (fileLines.get(j).contains("PPPPP")) {
                        pCount++;
                        pSteps.add(j);

                    }

                }

                if (pCount > 0 && (pCount % 2) == 0)
                    MainUI.filesOperated.add(banFiles.get(i));

                else
                    continue;

                pSteps.add(fileLines.size() - 1);

                for (int x = 0; x < pSteps.size(); x++) {
                    System.out.println(pSteps.get(x));
                }
                deletePairParts(fileLines, banFiles.get(i), pSteps);

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fileLines != null)
                fileLines.clear();
        }
    }

    @Deprecated
    private static void deletePairParts(ArrayList<String> fileLines, File file, ArrayList<Integer> pSteps) {
        ArrayList<String> finalData = new ArrayList<>();
        int x = 0;
        int y = 1;
        int iterator = 0;
        int first = pSteps.get(x);
        int last = pSteps.get(y);

        pSteps.add(999999999);
        pSteps.add(999999999);


        System.out.println("first = " + first + ", last = " + last);
        while (iterator < (pSteps.size() / 2) - 1) {
            for (int i = first; i < last; i++) {
                finalData.add(fileLines.get(i));
            }
            x = x + 2;
            y = y + 2;
            first = pSteps.get(x) + 1;
            last = pSteps.get(y);
            iterator++;
            System.out.println("first = " + first + ", last = " + last + " , iterator = " + iterator);
        }

        finalData.add(fileLines.get(fileLines.size() - 1));


        try {
            overriteFile(finalData, file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /* deletePartMode - new functionality
    Adding some data to pairs
    take a pair PPPPP and first 3 lines before that, and 2 lines after that
    */
    public static void deletePartModeVER_5_3(ArrayList<File> banFiles) {
        for (int i = 0; i < banFiles.size(); i++) {
            System.out.println(banFiles.get(i).getName());
            ArrayList<String> fileLines = new ArrayList();
            ArrayList<String> finalData;
            ArrayList<Integer> pairIndex = new ArrayList<>();
            File file = banFiles.get(i);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();

                }
                reader.close();

                for (int p = 0; p < fileLines.size(); p++) {
                    if (fileLines.get(p).contains("PPPPP")) {
                        pairIndex.add(p);
                        System.out.println(p);
                    }
                }

                if (pairIndex.size() % 2 != 0)
                    continue;
                else
                    MainUI.filesOperated.add(banFiles.get(i));

                finalData = deletePairPartsVER_5_3(fileLines, pairIndex);

                overriteFile(finalData, banFiles.get(i));

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (fileLines != null)
                fileLines.clear();

        }
    }


    private static ArrayList<String> deletePairPartsVER_5_3(ArrayList<String> fileLines, ArrayList<Integer> pairIndex) {

        String prefix = "%.";

        // subtracting the odd index with 3 and adding even index with 2 for a new pair indexes
        for(int i=0; i<pairIndex.size(); i++) {
            int index = (i%2==0) ? pairIndex.get(i)-3 : pairIndex.get(i)+2;
            pairIndex.set(i,index);
        }

        // adding extra indexes to handle loose ends
        pairIndex.add(999999999);
        pairIndex.add(999999999);

        // initial pointers
        int x = 0;
        int y = 1;
        int iterator = 0;
        int first = pairIndex.get(x);
        int last = pairIndex.get(y);

        // appending data over the pairs
        while (iterator < (pairIndex.size() / 2) - 1) {
            for (int i = first; i <=last; i++)
                fileLines.set(i,prefix+fileLines.get(i));

            x = x + 2;
            y = y + 2;
            first = pairIndex.get(x);
            last = pairIndex.get(y);
            iterator++;
        }

        return fileLines;
    }


    public static void PairSequencerMode(ArrayList<File> banFiles) {
        for (int i = 0; i < banFiles.size(); i++) {
            System.out.println(banFiles.get(i).getName());
            ArrayList<String> fileLines = new ArrayList();
            ArrayList<String> finalData = new ArrayList();
            ArrayList<Integer> pairIndex = new ArrayList<>();
            File file = banFiles.get(i);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();

                }
                reader.close();

                for (int p = 0; p < fileLines.size(); p++) {
                    if (fileLines.get(p).contains("PAIR")) {
                        pairIndex.add(p);
                        System.out.println(p);
                    }


                }

                if (pairIndex.size() % 2 != 0)
                    continue;
                else
                    MainUI.filesOperated.add(banFiles.get(i));

                finalData = PairSequencing(fileLines, pairIndex);

                overriteFile(finalData, banFiles.get(i));

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (fileLines != null)
                fileLines.clear();

        }
    }

    private static ArrayList<String> PairSequencing(ArrayList<String> fileLines, ArrayList<Integer> pairIndex) {
        ArrayList<String> finalData = new ArrayList<>();
        ArrayList<String> afterFinalData = new ArrayList<>();
        ArrayList<Integer> nonPairIndex = new ArrayList<>();
        fileLines.add("\n");
        fileLines.add("\n");
        fileLines.add("\n");

        nonPairIndex.add(0);
        for (int k = 0; k < pairIndex.size(); k++) {
            if (k % 2 == 0)
                nonPairIndex.add(pairIndex.get(k) - 1);
            else
                nonPairIndex.add(pairIndex.get(k) + 1);
        }
        nonPairIndex.add(fileLines.size() - 1);

        Collections.reverse(pairIndex);

        System.out.println("NON PAIR INDEXES");
        for (int l = 0; l < nonPairIndex.size(); l++)
            System.out.println(nonPairIndex.get(l));

        System.out.println("AFTER REVERSING PAIR INDEXES");
        for (int l = 0; l < pairIndex.size(); l++)
            System.out.println(pairIndex.get(l));

        for (int i = 0; i < pairIndex.size(); i = i + 2) {

            for (int x = nonPairIndex.get(i); x <= nonPairIndex.get(i + 1); x++) {
                finalData.add(fileLines.get(x));
            }
            System.out.println("NONPAIR INDEX");
            System.out.println(nonPairIndex.get(i));
            System.out.println(nonPairIndex.get(i + 1));

            for (int x = pairIndex.get(i + 1); x <= pairIndex.get(i); x++) {
                finalData.add(fileLines.get(x));
            }
            System.out.println("PAIR INDEX");
            System.out.println(pairIndex.get(i + 1));
            System.out.println(pairIndex.get(i));
        }

        for (int i = nonPairIndex.get(nonPairIndex.size() - 2); i <= nonPairIndex.get(nonPairIndex.size() - 1); i++) {
            finalData.add(fileLines.get(i));
        }
//        System.out.println("NONPAIR INDEX");
//        System.out.println(nonPairIndex.get(nonPairIndex.get(nonPairIndex.size() -1)));
//        System.out.println(nonPairIndex.get(nonPairIndex.get(nonPairIndex.size() - 2)));

        for (int i = 0; i < finalData.size(); i++) {
            if (finalData.get(i).contains("PAIR"))
                afterFinalData.add("% 000");
            else
                afterFinalData.add(finalData.get(i));
        }

        return afterFinalData;
    }

    public static void removeYvalues(ArrayList<File> banFiles) {
        for (int i = 0; i < banFiles.size(); i++) {
            System.out.println(banFiles.get(i).getName());
            ArrayList<String> fileLines = new ArrayList();
            ArrayList<String> finalData = new ArrayList();
            ArrayList<Integer> pairIndex = new ArrayList<>();
            File file = banFiles.get(i);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();

                }
                reader.close();
                String pattern = "Y[0-9]+[.][0-9]+";

                if (pairIndex.size() % 2 != 0)
                    continue;
                else
                    MainUI.filesOperated.add(banFiles.get(i));

                for (int p = 0; p < fileLines.size(); p++) {
                    if (fileLines.get(p).contains("X") && fileLines.get(p).contains("Y")) {

                        int indexYDot = fileLines.get(p).lastIndexOf('.');
                        finalData.add(fileLines.get(p).substring(0, indexYDot + 7));
                    } else {
                        finalData.add(fileLines.get(p));
                    }

                }

                // need to delete unnecessary lines - only one line gap

                ArrayList<String> finalDataAfterRemovingUnnecessaryLines = deleteUnnecessaryLines(finalData);

                overriteFile(finalDataAfterRemovingUnnecessaryLines, banFiles.get(i));

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (fileLines != null)
                fileLines.clear();

        }
    }


    public static ArrayList<String> deleteUnnecessaryLines(ArrayList<String> data) {
        int flag = 0;
        ArrayList<String> finalData = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).equals("")) {
                if (flag == 0) {
                    finalData.add(data.get(i));
                    flag++;
                } else {
                    flag++;
                }
            } else {
                finalData.add(data.get(i));
                flag = 0;
            }


        }
        return finalData;
    }


    public static void incrementByValues(ArrayList<File> banFiles, Double valueToBeIncrementedBy) {
        for (int i = 0; i < banFiles.size(); i++) {
            ArrayList<String> fileLines = new ArrayList();
            ArrayList<String> finalData = new ArrayList();
            ArrayList<Integer> pairIndexForUpToDown = new ArrayList<>();
            ArrayList<Integer> pairIndexForDownToUp = new ArrayList<>();
            File file = banFiles.get(i);
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();

                }
                reader.close();

                // checking the pairs for QQQQQ (up to down) and WWWWW (down to up)
                for (int p = 0; p < fileLines.size(); p++) {
                    if (fileLines.get(p).contains("QQQQQ")) {
                        pairIndexForUpToDown.add(p);
                    } else if (fileLines.get(p).contains("WWWWW")) {
                        pairIndexForDownToUp.add(p);
                    }
                }

                if (pairIndexForDownToUp.size() > 0 || pairIndexForUpToDown.size() > 0) {
                    if (pairIndexForDownToUp.size() % 2 != 0 && pairIndexForUpToDown.size() % 2 != 0)
                        continue;
                    else
                        MainUI.filesOperated.add(banFiles.get(i));
                }


                finalData = incrementValueUpwardsAndDownwardModes(fileLines,
                        pairIndexForUpToDown, pairIndexForDownToUp, valueToBeIncrementedBy);

                overriteFile(finalData, banFiles.get(i));

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (fileLines != null)
                fileLines.clear();

        }
    }

    public static ArrayList<String> incrementValueUpwardsAndDownwardModes(ArrayList<String> fileLines,
                                                                          ArrayList<Integer> pairIndexUpToDown,
                                                                          ArrayList<Integer> pairIndexDownToUp,
                                                                          Double valueToBeIncrementedBy) {
        ArrayList<String> finalData = fileLines;
        DecimalFormat df = new DecimalFormat("0.00");

        if (pairIndexUpToDown.size() % 2 == 0 && pairIndexUpToDown.size() > 0) {
            // operating 'up to down' mode - QQQQQ
            for (int i = 0; i < pairIndexUpToDown.size(); i = i + 2) {
                // cutting 2.7 from Z2.7 X23424
                String str = finalData.get(pairIndexUpToDown.get(i));
                String strEdited = str.substring(1, str.indexOf(' '));
                String initialLetter = str.substring(0, 1); // Z in this case
                Double startValue = Double.parseDouble(df.format(Double.parseDouble(strEdited))); // 2.70 in this case

                int multiplierCount = 1;
                for (int j = pairIndexUpToDown.get(i); j <= pairIndexUpToDown.get(i + 1); j++) {
                    Double value = Double.parseDouble(df.format(startValue + (valueToBeIncrementedBy * multiplierCount)));
                    String firstPart = initialLetter + value;
                    String secondPart = finalData.get(j).substring(finalData.get(j).indexOf(" "));
                    finalData.set(j, firstPart + secondPart);
                    multiplierCount++;
                }
            }

            finalData = removeSequenceOfCharactersFromEnd("QQQQQ", finalData);
        }

        if (pairIndexDownToUp.size() % 2 == 0 && pairIndexDownToUp.size() > 0) {
            // operating 'down to up' mode - WWWWW
            for (int i = 0; i < pairIndexDownToUp.size(); i = i + 2) {
                String str = finalData.get(pairIndexDownToUp.get(i + 1));
                String strEdited = str.substring(1, str.indexOf(' '));
                String initialLetter = str.substring(0, 1); // Z in this case
                Double startValue = Double.parseDouble(df.format(Double.parseDouble(strEdited))); // 2.70 in this case

                int multiplierCount = 1;
                for (int j = pairIndexDownToUp.get(i + 1); j >= pairIndexDownToUp.get(i); j--) {
                    Double value = Double.parseDouble(df.format(startValue + (valueToBeIncrementedBy * multiplierCount)));
                    String firstPart = initialLetter + value;
                    String secondPart = finalData.get(j).substring(finalData.get(j).indexOf(" "));
                    finalData.set(j, firstPart + secondPart);
                    multiplierCount++;
                }
            }
            finalData = removeSequenceOfCharactersFromEnd("WWWWW", finalData);
        }


        return finalData;
    }

    public static ArrayList<String> removeSequenceOfCharactersFromEnd(String charactersToBeRemoved,
                                                         ArrayList<String> fileLines)
    {
        for(int i=0; i<fileLines.size(); i++)
        {
            if(fileLines.get(i).contains(charactersToBeRemoved))
            {
                String subString = fileLines.get(i).substring(0,fileLines.get(i).indexOf(charactersToBeRemoved));
                fileLines.set(i,subString);
            }
        }

        return fileLines;
    }

    public static void replaceValuesAfterZ(ArrayList<File> banFiles, String valueToBeSet) {
        for (int i = 0; i < banFiles.size(); i++) {
            ArrayList<String> fileLines = new ArrayList<>();
            ArrayList<String> finalData = new ArrayList<>();
            ArrayList<Integer> pairIndexes = new ArrayList<>();
            File file = banFiles.get(i);
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = reader.readLine();
                while (line != null) {
                    fileLines.add(line);
                    line = reader.readLine();
                }
                reader.close();
                for (int p = 0; p < fileLines.size(); p++) {
                    if (((String)fileLines.get(p)).contains("RRRRR"))
                        pairIndexes.add(Integer.valueOf(p));
                }
                if (pairIndexes.size() > 0 && pairIndexes.size() % 2 == 0) {
                    MainUI.filesOperated.add(banFiles.get(i));
                    finalData = replaceValuesAfterZMode(fileLines, pairIndexes, valueToBeSet);
                    overriteFile(finalData, banFiles.get(i));
                } else {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (fileLines != null)
                fileLines.clear();
            continue;
        }
    }

    public static ArrayList<String> replaceValuesAfterZMode(ArrayList<String> fileLines, ArrayList<Integer> pairIndexes, String valueToBeSet) {
        ArrayList<String> finalData = fileLines;
        int i;
        for (i = 0; i < pairIndexes.size(); i += 2) {
            for (int j = ((Integer)pairIndexes.get(i)).intValue(); j <= ((Integer)pairIndexes.get(i + 1)).intValue(); j++) {
                String line = finalData.get(j);
                String stringToBeReplaced = line.substring(1, line.indexOf(' '));
                String finalLine = ((String)finalData.get(j)).replaceFirst(stringToBeReplaced, valueToBeSet);
                finalData.set(j, finalLine);
            }
        }
        return finalData;
    }

}
