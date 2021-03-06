package Entrega2;

import java.io.*;

import Utilities.CommonUtilities;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;

public class GetBaselineModel {

    /**
     * Genera un modelo NaiveBayes optimizado para un conjunto de instancias dado.
     *
     * @param args  Parámetros de entrada. En caso de no introducir ninguno se muestra una descripción de estos.
     */
    public static void main(String[] args) {
        String trainPath = null;
        String modelPath = null;
        try{
            if (args.length == 0){
                System.out.println("=====Get Baseline Model=====");
                System.out.println("Este programa tiene como función generar un modelo NaiveBayes óptimo dado un conjunto de instancias.");
                System.out.println("PRECONDICIÓN: El path donde se desea guardar el modelo debe ser un directorio.");
                System.out.println("POSCONDICIÓN: Se han creado tanto baseline.model como baseline_quality.txt.\n");
                System.out.println("Lista de argumentos:\n-Path del conjunto de datos de entrenamiento del modelo.\n" +
                                                 "-Path donde se desea guardar tanto el modelo como su calidad estimada.");
                System.out.println("Ejemplo de una correcta ejecución: java -jar GetBaselineModel.jar /path/to/data.arff /path/to/destination/");

                        System.exit(0);
            }else if (args.length == 2) {
                trainPath = args[0];
                modelPath = args[1];
            }else{
                CommonUtilities.printlnError("Error en los argumentos.");
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        Instances train = CommonUtilities.loadArff(trainPath, -1);
        Classifier optimalClassifier = getBaselineModel(train);
        CommonUtilities.saveModel(optimalClassifier, modelPath + train.relationName() + "_baseline.model");

        writeResults(optimalClassifier, train, modelPath);

    }

    /**
     * Este método obtiene un modelo NaiveBayes optimizado en base a f-measure para clasificar el conjunto de instancias
     * que se reciben como parámetro.
     *
     * @param inputTrain    Instancias que se desea clasificar.
     * @return              Modelo NaiveBayes óptimo.
     */
    private static NaiveBayes getBaselineModel(Instances inputTrain) {

        int minClassIndex = CommonUtilities.getMinorityClassIndex(inputTrain);

        boolean useKernelEstimator = false;
        boolean useSupervisedDiscretization = false;

        boolean optimalKernelEstimator = false;
        boolean optimalSupervisedDiscretization = false;
        Double fMeasureMax = 0.00;

        NaiveBayes naiveBayes = new NaiveBayes();

        System.out.println("Iniciando optimización. Esto puede llevar un tiempo.");
        for (int i = 1; i <= 2; i++) {
            for (int j = 1; j <= 2; j++) {
                try {
                    System.out.println("Optimización " + ((2*i+j)-2) + " de 4...");
                    naiveBayes.setUseSupervisedDiscretization(useSupervisedDiscretization);
                    naiveBayes.setUseKernelEstimator(useKernelEstimator);

                    Evaluation ev = CommonUtilities.evalKFoldCrossValidation(naiveBayes, inputTrain, 8,1);
                    if (fMeasureMax < ev.fMeasure(minClassIndex)) {
                        optimalKernelEstimator = useKernelEstimator;
                        optimalSupervisedDiscretization = useSupervisedDiscretization;
                        fMeasureMax = ev.fMeasure(minClassIndex);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                useSupervisedDiscretization = true;
            }

            useKernelEstimator = true;
        }

        try{
            System.out.println("=================================================");
            System.out.println("CONFIGURACIÓN ÓPTIMA DE NAIVE BAYES:");
            System.out.println("¿Utilizar kernelEstimator?             " + optimalKernelEstimator);
            System.out.println("¿Utilizat supervisedDiscretization?    " + optimalSupervisedDiscretization);
            System.out.println("=================================================");
            naiveBayes.setUseKernelEstimator(optimalKernelEstimator);
            naiveBayes.setUseSupervisedDiscretization(optimalSupervisedDiscretization);
            naiveBayes.buildClassifier(inputTrain);
        }catch (Exception e){
            e.printStackTrace();
        }

        return naiveBayes;
    }

    /**
     * Este método escribe en un archivo la calidad estimada del modelo NaiveBayes dado, siendo ésta calculada por medio
     * de 8-fold cross-validation.
     *
     * @param classifier    Modelo clasificador a usar.
     * @param instances     Conjunto de instancias de test.
     * @param pathOut       Ruta al fichero donde se desea guardar los resultados.
     */
    private static void writeResults(Classifier classifier, Instances instances, String pathOut) {
        try {
            System.out.println("Escritura de los resultados...");
            Evaluation eval = CommonUtilities.evalKFoldCrossValidation(classifier, instances, 8, 1);
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(pathOut + instances.relationName() + "_baseline_quality.txt"))));
            writer.println("==========================================================================================================");
            writer.println("CALIDAD ESTIMADA DE BASELINE:");
            writer.println("Se ha usado 8-fold cross-validation sobre el modelo baseline NaiveBayes para obtener la calidad estimada.");
            writer.println("==========================================================================================================");
            writer.println(eval.toSummaryString());
            writer.println(eval.toMatrixString());
            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}