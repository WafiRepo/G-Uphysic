package de.rwth_aachen.phyphox.Helper;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.model.QuestionModel;

public class AdvancedQuestionHelper {
    public static List<QuestionModel> generateQuestionList() {
        List<QuestionModel> list = new ArrayList<>();

        // Question 1
        list.add(new QuestionModel(
                "Mengingat bahwa kipas 1 dan kipas 2 beroperasi dalam kondisi stabil, bagaimana perbedaan percepatan sentripetal (a) antara kedua kipas tersebut, jika kipas 1 memiliki jari-jari (r) 0.13 m dan kipas 2 memiliki jari-jari (r) 0.17 m, serta mempertimbangkan kecepatan sudut (ω) masing-masing?",
                Arrays.asList(R.drawable.advanced_1)
                , "Advanced"
        ));

        // Question 2
        list.add(new QuestionModel(
                "Berdasarkan dua gambar anotasi kipas listrik dan diketahui bahwa kipas 1 memiliki jari-jari 0.13 m dan kipas 2 memiliki jari-jari 0.17 m, analisis bagaimana perbedaan jari-jari (r) memengaruhi percepatan sentripetal (a) dan kecepatan sudut (ω) masing-masing kipas saat dalam kondisi stabil.",
                Arrays.asList(R.drawable.advanced_2)
                , "Advanced"
        ));

        // Question 3
        list.add(new QuestionModel(
                "Berdasarkan gambar anotasi yang menunjukkan konsep percepatan sentripetal (a), kecepatan sudut (ω), dan jari-jari (r), bagaimana perbedaan percepatan sentripetal dan kecepatan sudut antara kipas 1 dan kipas 2 dalam kondisi stabil, mengingat jari-jari keduanya adalah 0.13 m dan 0.17 m?",
                Arrays.asList(R.drawable.advanced_3)
                , "Advanced"
        ));

        // Question 4
        list.add(new QuestionModel(
                "Dalam dua eksperimen,kipas 1 memiliki jari-jari (r) 0.13 m dan kipas 2 memiliki jari-jari (r) 0.17 m. Dapatkah kamu membandingkan kecepatan sudut (ω) kedua kipas dan menjelaskan mengapa salah satunya mungkin memiliki kecepatan sudut yang lebih tinggi? Jika keduanya berputar dengan kecepatan sudut yang sama, kipas mana yang menghasilkan percepatan sentripetal (a) lebih besar, dan apa alasannya? Pastikan untuk menghapus 5 data awal dan akhir sebelum melakukan perbandingan untuk kestabilan data.",
                Arrays.asList(R.drawable.advanced_4)
                , "Advanced"
        ));

        // Question 5
        list.add(new QuestionModel(
                "Diberikan bahwa Kipas 1 dan 2 memiliki kecepatan sudut (ω) yang sama pada waktu tertentu, namun kipas 1 (r = 0.13 m) mengalami percepatan sentripetal yang berbeda dengan kipas 2 (r = 0.17 m). Mengapa percepatan sentripetal (a) bisa berbeda antara keduanya, dan kipas mana yang mengalami percepatan lebih besar? Sertakan peran jari-jari (r) dan pastikan kondisi data stabil.",
                Arrays.asList(R.drawable.advanced_5)
                , "Advanced"
        ));

        // Question 6
        list.add(new QuestionModel(
                "Berdasarkan dua eksperimen, Kipas 1 (r = 0.13 m) dan kipas 2 (r = 0.17 m) menunjukkan laju peningkatan percepatan sentripetal (a) dan kecepatan sudut (ω) yang berbeda. Setelah menghapus 5 data awal dan akhir, kipas mana yang mengalami puncak percepatan sentripetal lebih tinggi dan bagaimana peran jari-jari dan perubahan ω terhadap hasil ini?",
                Arrays.asList(R.drawable.advanced_6)
                , "Advanced"
        ));

        // Question 7
        list.add(new QuestionModel(
                "Selama periode stabil dari 5s hingga 15s, kecepatan sudut Kipas 1 berfluktuasi antara 5.5–6.5 rad/s, sedangkan Kipas 2 stabil pada 7.5–8.5 rad/s. Jika r kipas 1 adalah 0.13 m dan r kipas 2 adalah 0.17 m, kipas mana yang memiliki percepatan sentripetal lebih besar?",
                Arrays.asList(R.drawable.advanced_7)
                , "Advanced"
        ));

        // Question 8
        list.add(new QuestionModel(
                "Dalam periode stabil kedua eksperimen, bagaimana perubahan jari-jari (r) antara kipas 1 (0.13 m) dan kipas 2 (0.17 m) memengaruhi keseimbangan antara percepatan sentripetal (a) dan kecepatan sudut (ω)?",
                Arrays.asList(R.drawable.advanced_8)
                , "Advanced"
        ));

        // Question 9
        list.add(new QuestionModel(
                "Selama periode stabil, Electric Fan 1 memiliki ω sekitar 6 rad/s, dan Electric Fan 2 sekitar 8 rad/s. Dengan r masing-masing 0.13 m dan 0.17 m, kipas mana yang memiliki a lebih besar?",
                Arrays.asList(R.drawable.advanced_9)
                , "Advanced"
        ));

        // Question 10
        list.add(new QuestionModel(
                "Jika jari-jari roda sepeda kiri adalah 0.25 m dan roda kanan adalah 0.20 m, dan keduanya berputar pada ω yang sama, bagaimana perbandingan percepatan sentripetal (a) antara keduanya?",
                Arrays.asList(R.drawable.advanced_10)
                , "Advanced"
        ));

        // Question 11
        list.add(new QuestionModel(
                "Dengan mempertimbangkan a, ω, dan r pada kedua gambar, jika Roda Sepeda 1 (r = 0.25 m) berputar lebih lambat dari Roda Sepeda 2 (r = 0.2 m), roda mana yang memerlukan a lebih besar untuk mempertahankan lintasannya?",
                Arrays.asList(R.drawable.advanced_11)
                , "Advanced"
        ));

        // Question 12
        list.add(new QuestionModel(
                "Jika seorang siswa mengamati eksperimen dengan cermat, mereka akan mencatat bahwa selama periode keadaan stabil (10 hingga 15 detik untuk kedua sepeda, dan 30 hingga 50 detik untuk Bicycle 1), kecepatan sudut (ω) tetap konstan. Mengingat bahwa Bicycle 1 memiliki jari-jari (r) yang lebih besar daripada Bicycle 2, bagaimana perbedaan jari-jari ini akan memengaruhi percepatan sentripetal (a) masing-masing sepeda selama periode keadaan stabil tersebut, dengan asumsi kecepatan sudut (ω) sama untuk keduanya?",
                Arrays.asList(R.drawable.advanced_12)
                , "Advanced"
        ));

        // Question 13
        list.add(new QuestionModel(
                "Jika seorang teman sekelas mengamati Bicycle Wheel 1 dan Bicycle Wheel 2 selama fase stabil mereka (10-20s), dan memperhatikan bahwa Bicycle Wheel 1 (dengan jari-jari (r) sebesar 0.25 m) mempertahankan percepatan sentripetal (a) sekitar 6 hingga 7 m/s², sementara Bicycle Wheel 2 (dengan jari-jari (r) sebesar 0.2 m) berfluktuasi antara 8 dan 12 m/s², dapatkah teman sekelas tersebut menyimpulkan bahwa roda dengan jari-jari yang lebih kecil (r) cenderung memiliki kecepatan sudut (ω) yang lebih tinggi?",
                Arrays.asList(R.drawable.advanced_13)
                , "Advanced"
        ));

        // Question 14
        list.add(new QuestionModel(
                "Selama eksperimen, kedua roda sepeda berputar dengan kecepatan sudut yang berbeda, dan Bicycle Wheel 1, yang memiliki jari-jari (r) lebih besar, menunjukkan percepatan sentripetal (a) yang lebih rendah dibandingkan dengan Bicycle Wheel 2. Dapatkah Anda menganalisis alasan mengapa roda dengan jari-jari lebih besar menunjukkan nilai (a) yang lebih rendah, terutama selama kondisi keadaan stabil yang diamati dalam eksperimen?",
                Arrays.asList(R.drawable.advanced_14)
                , "Advanced"
        ));

        // Question 15
        list.add(new QuestionModel(
                "Anda sedang memeriksa data dari dua eksperimen roda sepeda. Selama fase stabil—10–20 detik untuk Eksperimen 1 dan 10–30 detik untuk Eksperimen 2—percepatan sentripetal (a) pada Eksperimen 1 tetap sekitar 8 m/s², sementara pada Eksperimen 2 berfluktuasi antara 8 dan 12 m/s². Mengingat bahwa Bicycle Wheel 1 memiliki jari-jari (r) sebesar 0.25 m dan Bicycle Wheel 2 memiliki jari-jari lebih kecil sebesar 0.2 m, bagaimana perbedaan jari-jari (r) dapat membantu menjelaskan variasi percepatan sentripetal (a) antara kedua eksperimen?",
                Arrays.asList(R.drawable.advanced_15)
                , "Advanced"
        ));

        // Question 16
        list.add(new QuestionModel(
                "Dalam eksperimen yang melibatkan Bicycle Wheel 1 dan Bicycle Wheel 2, data dikumpulkan mengenai kecepatan sudut (ω) dan percepatan sentripetal (a) mereka dari waktu ke waktu. Wheel 1, dengan jari-jari sebesar 0.25 m, secara konsisten menunjukkan nilai (a) dan (ω) yang lebih rendah dibandingkan Wheel 2, yang memiliki jari-jari sedikit lebih kecil yaitu 0.2 m. Kedua roda mengalami fase di mana (a) dan (ω) menjadi stabil atau berfluktuasi, yang menunjukkan pola gerakan dinamis. Mengingat tren ini, faktor fisik apa—seperti ukuran roda, variasi kecepatan, atau hambatan—yang mungkin menjelaskan mengapa Wheel 2 secara konsisten menunjukkan nilai (a) dan (ω) yang lebih tinggi?",
                Arrays.asList(R.drawable.advanced_1)
                , "Advanced"
        ));

        // Question 17
        list.add(new QuestionModel(
                "Analisis data eksperimen yang dikumpulkan dari dua roda sepeda, satu dengan jari-jari (r) sebesar 0.25 m dan yang lain sebesar 0.2 m, dengan fokus pada bagaimana percepatan sentripetal (a) berubah sehubungan dengan kecepatan sudut (ω). Setelah menghapus lima nilai pertama dan lima nilai terakhir untuk mengisolasi data stabil, amati apakah Dataset 2, dengan nilai (a) dan (ω) yang lebih tinggi, mencerminkan usaha pengendara yang lebih besar atau faktor lingkungan seperti permukaan jalan atau daya cengkeram. Dengan Dataset 1 mencapai 7.73 m/s² dan 6.94 rad/s, dan Dataset 2 mencapai puncak 13.03 m/s² dan 8.05 rad/s, kesimpulan apa yang dapat Anda tarik mengenai peran jari-jari (r) dalam hasil ini?",
                Arrays.asList(R.drawable.advanced_17)
                , "Advanced"
        ));

        // Question 18
        list.add(new QuestionModel(
                "Mengingat bahwa jari-jari (r) dari rotating display di sebelah kiri adalah 0.14 meter dan jari-jari di sebelah kanan adalah 0.11 meter, dan dengan asumsi bahwa kedua rotating display berputar dengan kecepatan sudut (ω) yang sama, bagaimana perbedaan percepatan sentripetal (a) antara keduanya? Pertimbangkan peran jari-jari dalam menentukan percepatan sentripetal ketika kecepatan sudut tetap konstan.",
                Arrays.asList(R.drawable.advanced_18)
                , "Advanced"
        ));

        // Question 19
        list.add(new QuestionModel(
                "Jika Rotating Display 1 (r = 0.14 m) berputar lebih lambat dari Display 2 (r = 0.11 m), mana yang memerlukan a lebih besar? Jelaskan berdasarkan arah ω dan a.",
                Arrays.asList(R.drawable.advanced_19)
                , "Advanced"
        ));

        // Question 20
        list.add(new QuestionModel(
                "Anda sedang menganalisis dua eksperimen yang melibatkan Rotating Display 1 (radius = 0.11 m) dan Rotating Display 2 (radius = 0.14 m). Dalam kedua eksperimen, percepatan sentripetal (a) dan kecepatan sudut (ω) meningkat seiring waktu, tetapi Rotating Display 2 mencapai nilai maksimum yang lebih tinggi. Setelah mengabaikan lima data awal dan lima data akhir untuk fokus pada wilayah stabil, bagaimana perbedaan radius (r) memengaruhi laju dan besarnya peningkatan baik pada (a) maupun (ω)? Apakah periode stabil berbeda antara kedua display, dan apa yang hal tersebut ungkapkan tentang hubungan antara (r), (a), dan (ω)?",
                Arrays.asList(R.drawable.advanced_20)
                , "Advanced"
        ));

        // Question 21
        list.add(new QuestionModel(
                "Anda dan teman sekelas ditugaskan untuk mempelajari dua Rotating Display plate yang berbeda, Rotating Display plate 1 dan 2, untuk proyek fisika Anda. Display plate 1 memiliki jari-jari (r) sebesar 0.11 m, sedangkan Display plate 2 memiliki jari-jari (r) sebesar 0.14 m. Setelah mengumpulkan data tentang percepatan sentripetal (a) dan kecepatan sudut (ω), tim Anda mengamati bahwa Display plate 2 secara konsisten menunjukkan nilai (a) dan (ω) yang lebih tinggi dibandingkan Display plate 1, terutama selama bagian tengah eksperimen. Dengan mengabaikan lima data awal dan lima data akhir untuk kestabilan, dapatkah Anda menjelaskan display mana yang berputar lebih cepat dan bagaimana perbedaan jari-jari (r) dapat memengaruhi hubungan antara kecepatan sudut (ω) dan percepatan sentripetal (a)?",
                Arrays.asList(R.drawable.advanced_21)
                , "Advanced"
        ));

        // Question 22
        list.add(new QuestionModel(
                "Selama fase stabil antara 5 dan 15 detik, seorang siswa mengamati bahwa Rotating Display 1 (dengan jari-jari r = 0.14 m) memiliki kecepatan sudut (ω) yang sedikit lebih tinggi dibandingkan Rotating Display 2 (dengan jari-jari r = 0.11 m). Jika tujuannya adalah untuk menjaga percepatan sentripetal (a) yang sama untuk kedua display selama fase stabil ini, apakah jari-jari (r) dari Rotating Display 2 sebaiknya ditingkatkan, dikurangi, atau dipertahankan? Gunakan hubungan antara jari-jari dan kecepatan sudut dari grafik untuk menjelaskan alasan Anda.",
                Arrays.asList(R.drawable.advanced_22)
                , "Advanced"
        ));

        // Question 23
        list.add(new QuestionModel(
                "Mengingat bahwa Rotating Display 1 (Exp1) memiliki jari-jari (r) sebesar 0.14 m dan Rotating Display 2 (Exp2) memiliki jari-jari (r) sebesar 0.11 m, dan mempertimbangkan kecepatan sudut (ω) stabil yang diamati antara 5 hingga 10 detik dalam grafik, rotating display mana yang akan mengalami percepatan sentripetal (a) yang lebih besar selama fase tersebut, dan alasan apa yang mendukung kesimpulan Anda?",
                Arrays.asList(R.drawable.advanced_23)
                , "Advanced"
        ));

        // Question 24
        list.add(new QuestionModel(
                "Bagaimana Anda menjelaskan perbedaan percepatan sentripetal (a) dan kecepatan sudut (ω) yang diamati dalam keadaan stabil dari kedua eksperimen, mengingat bahwa sepeda 1 memiliki jari-jari yang lebih besar (r=0.25m) dibandingkan dengan sepeda 2 (r=0.2m)? Harap analisis data titik-titik keadaan stabil dalam dataset: percepatan sentripetal tetap sekitar 7 m/s² antara t=7s dan t=9 untuk sepeda 1 dan sekitar 12 m/s² antara t=10s dan t=13s untuk sepeda 2.",
                Arrays.asList(R.drawable.advanced_24_1, R.drawable.advanced_24_2)
                , "Advanced"
        ));

        // Question 25
        list.add(new QuestionModel(
                "Bandingkan percepatan sentripetal (a) dan kecepatan sudut (ω) yang diamati dalam eksperimen sepeda 1 dan sepeda 2. Bagaimana perbedaan radius rotasi (r) memengaruhi hubungan antara percepatan sentripetal dan kecepatan sudut, mengingat bahwa sepeda 1 memiliki radius lebih besar (r=0.25m) dibandingkan sepeda 2 (r=0.2m)? Apa arti fluktuasi dalam percepatan sentripetal selama periode stabil dan fluktuatif mengenai kecepatan dan radius dalam kedua eksperimen, terutama sehubungan dengan percepatan sentripetal yang lebih tinggi yang diamati pada sepeda 2?",
                Arrays.asList(R.drawable.advanced_25_1, R.drawable.advanced_25_2)
                , "Advanced"
        ));

        // Question 26
        list.add(new QuestionModel(
                "Di antara dua kipas yang berputar dengan jari-jari berbeda (Exp 1: 0.13 m, Exp 2: 0.17 m), eksperimen mana yang menunjukkan peningkatan percepatan sentripetal (a) yang lebih efisien relatif terhadap kecepatan sudutnya (ω) selama fase peningkatan dan stabil, dan bagaimana pengaruh radius terhadap perbandingan ini?",
                Arrays.asList(R.drawable.advanced_26_1, R.drawable.advanced_26_2)
                , "Advanced"
        ));

        // Question 27
        list.add(new QuestionModel(
                "Bagaimana perbandingan dua kipas berputar (Exp 1: 0.13 m, Exp 2: 0.17 m) dalam hal percepatan sentripetal (a) dan kecepatan sudut (ω) di seluruh fase peningkatan, stabil, dan penurunan? Apa peran jari-jari dalam performa mereka, terutama selama fase stabil?",
                Arrays.asList(R.drawable.advanced_27_1, R.drawable.advanced_27_2)
                , "Advanced"
        ));

        // Question 28
        list.add(new QuestionModel(
                "Melihat keadaan stabil dari dataset kedua eksperimen dan gambar anotasi sepeda, jika seorang siswa mengendarai masing-masing sepeda (dengan jari-jari 0.25m untuk sepeda 1 dan 0.20m untuk sepeda 2), sambil mempertahankan kecepatan sudut yang serupa seperti yang diamati dalam eksperimen masing-masing, bagaimana perbandingan percepatan sentripetal (a) antara kedua sepeda tersebut? Dengan mempertimbangkan hubungan antara percepatan sentripetal, kecepatan sudut (ω), dan jari-jari (r), apakah sepeda dengan jari-jari lebih besar (sepeda 1) akan menunjukkan percepatan sentripetal yang lebih tinggi atau lebih rendah dibandingkan sepeda 2?",
                Arrays.asList(R.drawable.advanced_28_1, R.drawable.advanced_28_2)
                , "Advanced"
        ));

        // Question 29
        list.add(new QuestionModel(
                "Selama periode keadaan stabil 20–30 detik di kedua eksperimen, Anda memperhatikan bahwa meskipun kecepatan sudut serupa, sepeda menunjukkan percepatan sentripetal yang berbeda. Mengingat bahwa sepeda 1 memiliki radius roda sebesar 0.25 m dan sepeda 2 memiliki radius roda sebesar 0.20 m, bagaimana variasi jari-jari tersebut berkontribusi terhadap perbedaan percepatan sentripetal yang diamati antara kedua sepeda?",
                Arrays.asList(R.drawable.advanced_29_1, R.drawable.advanced_29_2)
                , "Advanced"
        ));

        // Question 30
        list.add(new QuestionModel(
                "Bagaimana perbedaan jari-jari antara kipas Exp1 (0.13 m) dan kipas Exp2 (0.17 m) memengaruhi pola kecepatan sudut (ω) yang diamati dalam setiap fase (peningkatan, stabil, dan penurunan), dan kesimpulan apa yang dapat ditarik dari perbedaan kinerja mereka?",
                Arrays.asList(R.drawable.advanced_30_1, R.drawable.advanced_30_2)
                , "Advanced"
        ));

        // Question 31
        list.add(new QuestionModel(
                "Bagaimana perbedaan jari-jari antara kipas Exp1 (0.13 m) dan kipas Exp2 (0.17 m) memengaruhi laju peningkatan, nilai puncak, dan penurunan kecepatan sudut (ω) seiring waktu, dan kesimpulan apa yang dapat ditarik tentang kinerja mekanis masing-masing?",
                Arrays.asList(R.drawable.advanced_31_1, R.drawable.advanced_31_2)
                , "Advanced"
        ));

        // Question 32
        list.add(new QuestionModel(
                "Mengingat keadaan stabil yang diamati dalam dataset dan grafik perbandingan, dapatkah Anda menganalisis mengapa sepeda 2 menunjukkan nilai percepatan sentripetal (a) dan kecepatan sudut (ω) yang lebih tinggi dibandingkan dengan sepeda 1, meskipun memiliki jari-jari (r) yang lebih kecil? Renungkan hubungan antara parameter-parameter ini dan jelaskan faktor-faktor yang menyebabkan perbedaan yang diamati dalam keadaan stabil ini.",
                Arrays.asList(R.drawable.advanced_32_1, R.drawable.advanced_32_2)
                , "Advanced"
        ));

        // Question 33
        list.add(new QuestionModel(
                "Anda dan teman-teman sekelas sedang meninjau hasil dua eksperimen yang melibatkan roda sepeda, dan Anda sedang memeriksa grafik perbandingan yang terkait dengan kedua eksperimen. Dalam setiap eksperimen, digunakan roda sepeda, dengan sepeda 1 memiliki radius 0.25 m dan sepeda 2 memiliki radius 0.2 m. Kecepatan sudut (ω) dan percepatan sentripetal (a) diukur selama periode stabil dalam kedua eksperimen. Selama periode stabil sepeda 1 (dari 10 hingga 13 detik), kecepatan sudut sekitar 5.8 rad/s, dan selama periode stabil sepeda 2, kecepatan sudut sekitar 7 rad/s. Mengingat perbedaan jari-jari (r) dan kecepatan sudut yang sesuai selama periode stabil dari masing-masing eksperimen, dapatkah Anda menentukan roda sepeda mana yang menghasilkan percepatan sentripetal lebih tinggi?",
                Arrays.asList(R.drawable.advanced_33_1, R.drawable.advanced_33_2)
                , "Advanced"
        ));

        // Question 34
        list.add(new QuestionModel(
                "Bagaimana tren kecepatan sudut (ω) dari kipas Exp1 dan kipas Exp2 berbeda di seluruh fase peningkatan, stabil, dan penurunan, dan apa yang hal ini ungkapkan tentang dampak jari-jari kipas terhadap kinerja rotasi seiring waktu?",
                Arrays.asList(R.drawable.advanced_34_1, R.drawable.advanced_34_2)
                , "Advanced"
        ));

        // Question 35
        list.add(new QuestionModel(
                "Mengingat bahwa kipas Exp2 (jari-jari = 0.17 m) mencapai puncak kecepatan sudut yang lebih tinggi sekitar 12.65 rad/s dan mempertahankannya lebih lama dibandingkan kipas Exp1 (jari-jari = 0.13 m, puncak ~7.81 rad/s), bagaimana perbedaan kinerja ini di seluruh fase gerakan mencerminkan pengaruh jari-jari terhadap perilaku rotasi, stabilitas, dan inersia?",
                Arrays.asList(R.drawable.advanced_35_1, R.drawable.advanced_35_2)
                , "Advanced"
        ));

        return list;
    }


    public static Pair<Integer, QuestionModel> getRandomQuestion() {
        List<QuestionModel> questions = generateQuestionList();
        Random rand = new Random();
        int index = rand.nextInt(questions.size());
        QuestionModel randomQuestion = questions.get(index);
        return new Pair<>(index, randomQuestion);
    }

}
