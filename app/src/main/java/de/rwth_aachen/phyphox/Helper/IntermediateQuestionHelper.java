package de.rwth_aachen.phyphox.Helper;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.model.QuestionModel;

public class IntermediateQuestionHelper {
    public static List<QuestionModel> generateQuestionList() {
        List<QuestionModel> list = new ArrayList<>();
        list.add(new QuestionModel(
                "Seorang siswa sedang mengamati outdoor fitness double wheel dan ingin menghitung percepatan sentripetal (a) dari salah satu roda. Ia mengetahui jari-jari lintasan melingkar roda tersebut adalah 0.43 m, tetapi belum mengetahui kecepatan sudut (ω). Jika ia mengetahui bahwa roda tersebut menyelesaikan satu putaran penuh dalam 0.2 detik, bisakah kamu membantunya menghitung kecepatan sudut terlebih dahulu, lalu menggunakannya untuk menentukan percepatan sentripetal?",
                Arrays.asList(R.drawable.intermediate_1),
                "Intermediate"
        ));
        list.add(new QuestionModel(
                "Kamu sedang mengamati outdoor fitness double wheel yang berputar dengan jari-jari (r) sebesar 0.43 m. Saat kecepatan rotasi meningkat, kecepatan sudut (ω) juga meningkat. Bisakah kamu menjelaskan hubungan antara percepatan sentripetal (a) dan kecepatan sudut (ω) di berbagai titik rotasi? Perhatikan juga pengaruh jari-jari (r) terhadap sistem ini.",
                Arrays.asList(R.drawable.intermediate_2)
                , "Intermediate"
        ));
        list.add(new QuestionModel(
                "Selama eksperimen laboratorium, siswa mengamati bahwa antara detik ke-10 hingga ke-17, kecepatan sudut (ω) dari outdoor fitness double wheel stabil di sekitar 7 rad/s. Jika jari-jari (r) roda adalah 0.43 m, bagaimana kamu memperkirakan perilaku percepatan sentripetal (a) selama periode ini? Apakah akan meningkat, menurun, atau tetap stabil?",
                Arrays.asList(R.drawable.intermediate_3)
                , "Intermediate"
        ));
        // Question 4
        list.add(new QuestionModel(
                "Josep mencatat bahwa pada periode stabil pertama di grafik (sekitar detik 10 hingga 17), kecepatan sudut (ω) dari roda outdoor fitness tetap konstan. Jika jari-jari lintasan roda adalah 0.43 m, apa yang terjadi pada percepatan sentripetal (a) jika kecepatan sudut (ω) digandakan selama periode ini? Apakah (a) juga akan menjadi dua kali lipat, tetap, menurun, atau menjadi empat kali lipat?",
                Arrays.asList(R.drawable.intermediate_4)
                , "Intermediate"
        ));

        // Question 5
        list.add(new QuestionModel(
                "Berdasarkan data performa roda outdoor fitness, pada fase stabil terbaru tercatat kecepatan sudut (ω) sebesar 4.58 rad/s pada waktu 19.98 detik dengan percepatan sentripetal (a) sebesar 9.8 m/s². Jika jari-jari roda adalah 0.43 m, apakah roda masih berada dalam fase stabil jika kecepatan sudut ditingkatkan menjadi 5.0 rad/s? Hitung percepatan sentripetal barunya dan jelaskan dampaknya terhadap kinerja roda.",
                Arrays.asList(R.drawable.intermediate_5)
                , "Intermediate"
        ));

        // Question 6
        list.add(new QuestionModel(
                "Apakah nilai percepatan sentripetal yang diamati dari outdoor fitness double wheel dengan jari-jari 0.43 m sesuai dengan hasil perhitungan rumus teoritis? Jika kecepatan sudut meningkat dari 0.00 rad/s menjadi 5.04 rad/s dan percepatan sentripetal naik dari 0.01 m/s² ke 13.97 m/s², apakah nilai teoritisnya mendekati hasil pengamatan?",
                Arrays.asList(R.drawable.intermediate_6)
                , "Intermediate"
        ));

        // Question 7
        list.add(new QuestionModel(
                "Jika jari-jari kipas angin adalah 0.17 m dan kecepatan sudut (ω) stabil pada 6.5 rad/s dari detik ke-10 hingga ke-15, bagaimana perubahan pada ω akan memengaruhi percepatan sentripetal (a)?",
                Arrays.asList(R.drawable.intermediate_7)
                , "Intermediate"
        ));

        // Question 8
        list.add(new QuestionModel(
                "Jika kecepatan sudut (ω) roda meningkat, bagaimana pengaruhnya terhadap percepatan sentripetal (a)? Hitung nilai a saat ω mencapai nilai maksimum dan bandingkan dengan nilai awal dan nilai tengah yang diamati.",
                Arrays.asList(R.drawable.intermediate_8)
                , "Intermediate"
        ));

        // Question 9
        list.add(new QuestionModel(
                "Selama periode stabil dari detik ke-13 hingga ke-18, jika percepatan sentripetal (a) tetap konstan dan jari-jari (r) kipas angin adalah 0.17 m, apa yang bisa terjadi pada kecepatan sudut (ω) kipas?",
                Arrays.asList(R.drawable.intermediate_9)
                , "Intermediate"
        ));

        // Question 10
        list.add(new QuestionModel(
                "Pada periode waktu dari 10 hingga 15 detik, kipas angin memiliki percepatan sentripetal (a) yang stabil sekitar 7 m/s². Jika jari-jari (r) kipas adalah 0.13 m, apa yang dapat kamu simpulkan tentang kecepatan sudut (ω) selama periode ini?",
                Arrays.asList(R.drawable.intermediate_10)
                , "Intermediate"
        ));

        // Question 11
        list.add(new QuestionModel(
                "Jika jari-jari (r) kipas angin adalah 0.17 m dan menyelesaikan satu putaran dalam 16.78 detik, berapa kecepatan sudut (ω) dalam rad/s?",
                Arrays.asList(R.drawable.intermediate_11)
                , "Intermediate"
        ));

        // Question 12
        list.add(new QuestionModel(
                "Baling-baling kipas berputar dengan kecepatan sudut (ω) sebesar 8.47 rad/s dan jari-jarinya 0.17 m. Jika kecepatan kipas meningkat, bagaimana kamu memperkirakan perubahan pada percepatan sentripetal (a)? Jelaskan alasanmu.",
                Arrays.asList(R.drawable.intermediate_12)
                , "Intermediate"
        ));

        // Question 13
        list.add(new QuestionModel(
                "Baling-baling kipas berputar dengan kecepatan sudut (ω) sebesar 8.47 rad/s dan jari-jarinya 0.17 m. Jika kecepatan kipas meningkat, bagaimana kamu memperkirakan perubahan pada percepatan sentripetal (a)? Jelaskan alasanmu.",
                Arrays.asList(R.drawable.intermediate_13)
                , "Intermediate"
        ));

        // Question 13
        list.add(new QuestionModel(
                "Dalam periode stabil dengan ω antara 7.51 rad/s dan 8.42 rad/s, dan a sekitar 12 m/s², bisakah kamu menghitung jari-jari lintasan baling-baling kipas angin? Abaikan lima data pertama dan terakhir sebelum memulai perhitungan.",
                Arrays.asList(R.drawable.intermediate_14)
                , "Intermediate"
        ));

        // Question 14
        list.add(new QuestionModel(
                "Pada periode stabil sekitar 8.19 hingga 20.72 detik, di mana percepatan sentripetal (a) stabil di sekitar 9.43 m/s², dan jari-jari kipas adalah 0.17 m, bisakah kamu menghitung kecepatan sudut (ω) selama interval ini dengan mengabaikan lima data awal dan lima data akhir untuk menjaga kestabilan?",
                Arrays.asList(R.drawable.intermediate_15)
                , "Intermediate"
        ));

        // Question 15
        list.add(new QuestionModel(
                "Selama periode stabil dari 8.19 s hingga 20.72 s, kecepatan sudut (ω) rata-rata sekitar 8.42 rad/s dan percepatan sentripetal (a) stabil di sekitar 12.40 m/s². Dengan hanya menggunakan 5 baris data terakhir dari fase stabil ini, bisakah kamu menentukan jari-jari (r) lintasan melingkar berdasarkan hubungan antara (ω) dan (a)?",
                Arrays.asList(R.drawable.intermediate_16)
                , "Intermediate"
        ));

        // Question 16
        list.add(new QuestionModel(
                "Kamu sedang mengamati sepeda dengan roda yang memiliki jari-jari (r) sebesar 0.25 m, dan roda tersebut berputar dengan kecepatan sudut (ω). Bagaimana perubahan pada kecepatan sudut memengaruhi percepatan sentripetal (a) roda?",
                Arrays.asList(R.drawable.intermediate_17)
                , "Intermediate"
        ));

        // Question 17
        list.add(new QuestionModel(
                "Kamu sedang mengamati roda sepeda yang berputar dengan jari-jari (r) 0.25 m dan kecepatan sudut (ω) yang meningkat. Bagaimana kamu menggambarkan hubungan antara kecepatan sudut (ω) dan percepatan sentripetal (a) selama fase percepatan roda?",
                Arrays.asList(R.drawable.intermediate_18)
                , "Intermediate"
        ));

        // Question 18
        list.add(new QuestionModel(
                "Kamu sedang menyelidiki perilaku roda sepeda. Kamu mengetahui bahwa kecepatan sudut maksimum roda (ω) adalah 6.94 rad/s dan percepatan sentripetal maksimum (a) adalah 7.73 m/s². Roda kemudian mencapai keadaan stabil dengan ω sebesar 6.59 rad/s dan a sebesar 6.22 m/s². Jika jari-jari roda adalah 0.25 m, tentukan kecepatan sudut awal saat percepatan sentripetal hanya 0.01 m/s².",
                Arrays.asList(R.drawable.intermediate_19)
                , "Intermediate"
        ));

        // Question 19
        list.add(new QuestionModel(
                "Kamu sedang melakukan eksperimen menggunakan roda sepeda dengan jari-jari 0.25 m. Selama eksperimen, kamu mencatat kecepatan sudut (ω) dan percepatan sentripetal (a) pada berbagai interval waktu. Pada awal eksperimen (1.29 s), ω dan a masing-masing adalah 0.01 rad/s dan 0.01 m/s². Pada titik tertentu (15.3 s), tercatat ω maksimum sebesar 6.94 rad/s. Nilai maksimum a tercatat pada 7.68 s sebesar 7.73 m/s². Menjelang akhir eksperimen (16.77 s), ω dan a tercatat 6.15 rad/s dan 5.85 m/s². Identifikasilah titik dalam eksperimen di mana percepatan sentripetal mulai menurun meskipun kecepatan sudut meningkat. Apa kemungkinan penyebab dari kejadian ini?",
                Arrays.asList(R.drawable.intermediate_20)
                , "Intermediate"
        ));

        // Question 20
        list.add(new QuestionModel(
                "Michael sedang mempelajari perilaku roda sepeda dengan jari-jari (r) 0.25 m. Selama periode stabil, ia mengamati bahwa percepatan sentripetal (a) tetap konstan di angka 7 m/s². Berdasarkan informasi ini, Michael ingin tahu apakah kecepatan sudut (ω) dari waist trainer meningkat, menurun, atau tetap selama periode ini.",
                Arrays.asList(R.drawable.intermediate_21)
                , "Intermediate"
        ));
        // Question 21
        list.add(new QuestionModel(
                "Jika dalam periode stabil dari detik ke-8 hingga ke-15 roda sepeda (r = 0.25 m) memiliki percepatan sentripetal konstan antara 7 hingga 8 m/s², apa yang akan terjadi pada percepatan sentripetal (a) jika kecepatan sudut (ω) dikurangi setengahnya?",
                Arrays.asList(R.drawable.intermediate_22), "Intermediate"
        ));

        // Question 22
        list.add(new QuestionModel(
                "Kamu sedang menggunakan waist trainer dengan jari-jari (r) sebesar 0.09 m dan mengamati bagaimana kecepatan sudut (ω) memengaruhi percepatan sentripetal (a). Ketika waist trainer mulai berputar lebih cepat, bagaimana kamu menggambarkan perubahan pada a terhadap ω di waktu yang berbeda? Apa yang terjadi pada a jika ω menurun?",
                Arrays.asList(R.drawable.intermediate_23), "Intermediate"
        ));

        // Question 23
        list.add(new QuestionModel(
                "Kamu menggunakan waist trainer dengan jari-jari (r) sebesar 0.09 m. Saat alat ini berputar lebih cepat, kamu mengamati perubahan pada kecepatan sudut (ω) dan percepatan sentripetal (a). Dapatkah kamu menjelaskan bagaimana a berperilaku ketika ω meningkat? Apa pengaruh perubahan jari-jari (r) terhadap a selama fase gerakan yang berbeda?",
                Arrays.asList(R.drawable.intermediate_24), "Intermediate"
        ));

        // Question 24
        list.add(new QuestionModel(
                "Sebuah waist trainer berputar pada kecepatan tertentu. Pada suatu titik, kecepatan sudut (ω) tercatat sebesar 10.22 rad/s dan percepatan sentripetal (a) sebesar 11.72 m/s². Jika jari-jari waist trainer adalah 0.09 m, dapatkah kamu menghitung apakah nilai-nilai tersebut konsisten dengan rumus percepatan sentripetal? Jika tidak konsisten, apa kemungkinan penyebabnya?",
                Arrays.asList(R.drawable.intermediate_25), "Intermediate"
        ));

        // Question 25
        list.add(new QuestionModel(
                "Berdasarkan data eksperimen waist trainer, bayangkan kamu seorang desainer yang ditugaskan untuk mengoptimalkan performa alat tersebut. Kamu melihat bahwa kecepatan sudut dan percepatan sentripetal berkaitan langsung. Dengan kecepatan sudut maksimum tercatat 14.79 rad/s dan percepatan sentripetal maksimum 16.36 m/s², serta jari-jari 0.09 m, bisakah kamu menghitung percepatan sentripetal secara teoritis pada kecepatan sudut maksimum dan membandingkannya dengan nilai yang tercatat?",
                Arrays.asList(R.drawable.intermediate_26), "Intermediate"
        ));

        // Question 26
        list.add(new QuestionModel(
                "Selama eksperimen, waist trainer menunjukkan percepatan sentripetal (a) yang stabil sekitar 15 m/s² antara detik ke-6 hingga ke-10. Jika jari-jari alat tersebut adalah 0.09 m, apakah kecepatan sudut (ω) juga tetap konstan selama periode ini? Mengapa kestabilan nilai a menunjukkan bahwa ω tidak berubah selama interval tersebut?",
                Arrays.asList(R.drawable.intermediate_27), "Intermediate"
        ));

        // Question 27
        list.add(new QuestionModel(
                "Mike sedang mempelajari perilaku waist trainer. Dari data yang ia peroleh, terlihat bahwa antara detik ke-8 hingga ke-10 dan ke-14 hingga ke-16, percepatan sentripetal (a) stabil sekitar 15 m/s². Jika jari-jari alat adalah 0.09 m, bisakah kamu membantu Mike memperkirakan berapa kecepatan sudut (ω) selama periode ini?",
                Arrays.asList(R.drawable.intermediate_28), "Intermediate"
        ));

        // Question 28
        list.add(new QuestionModel(
                "Kamu sedang mengamati outdoor fitness double wheel dengan jari-jari (r) sebesar 0.43 m, dan roda tersebut berputar dengan kecepatan sudut (ω). Ketika kecepatan rotasi berubah, bagaimana pengaruhnya terhadap percepatan sentripetal (a)? Dapatkah kamu menjelaskan bagaimana jari-jari memengaruhi gerakan sistem dan dampaknya terhadap percepatan sentripetal?",
                Arrays.asList(R.drawable.intermediate_29), "Intermediate"
        ));

        // Question 29
        list.add(new QuestionModel(
                "Kamu sedang mengamati waist trainer dengan jari-jari (r) sebesar 0.09 m. Ketika alat ini berputar, kecepatan sudutnya (ω) meningkat, menyebabkan percepatan sentripetal (a) juga berubah. Bagaimana kamu menggambarkan hubungan antara a dan ω dalam kasus ini jika jari-jari tetap?",
                Arrays.asList(R.drawable.intermediate_30), "Intermediate"
        ));

        // Question 30
        list.add(new QuestionModel(
                "Jika seorang siswa memiliki sebuah waist trainer dengan jari-jari bilah sebesar 0.09 m (r), dan selama periode waktu dari detik ke-5 hingga ke-13 kecepatan sudut (ω) tetap stabil sekitar 14 rad/s, bagaimana perilaku percepatan sentripetal (a) dari waist trainer selama periode tersebut?",
                Arrays.asList(R.drawable.intermediate_31), "Intermediate"
        ));

        // Question 31
        list.add(new QuestionModel(
                "Selama periode stabil antara detik ke-3 hingga ke-10, jika kecepatan sudut (ω) dari waist trainer tiba-tiba meningkat sementara jari-jari (r) tetap 0.09 m, apa yang akan terjadi pada percepatan sentripetal (a)? Mengapa perubahan tersebut terjadi? Bagaimana hubungan antara ω dan a menjelaskan fenomena ini?",
                Arrays.asList(R.drawable.intermediate_32), "Intermediate"
        ));

        // Question 32
        list.add(new QuestionModel(
                "Selama periode stabil di mana kecepatan sudut (ω) roda sepeda sebesar 6.5 rad/s dan jari-jari roda 0.25 m, apa yang terjadi terhadap percepatan sentripetal (a)? Mengapa percepatan sentripetal tetap konstan dalam kondisi ini? Bagaimana hubungan antara ω dan a menjelaskan fenomena tersebut?",
                Arrays.asList(R.drawable.intermediate_33), "Intermediate"
        ));

        // Question 33
        list.add(new QuestionModel(
                "Selama periode stabil dari detik ke-23 hingga ke-25, bilah roda sepeda berputar dengan kecepatan sudut (ω) konstan sebesar 3 rad/s. Jari-jari (r) roda adalah 0.25 m. Jika kamu meningkatkan kecepatan sudut (ω), bagaimana pengaruhnya terhadap percepatan sentripetal (a)? Apakah a akan meningkat, menurun, atau tetap?",
                Arrays.asList(R.drawable.intermediate_34), "Intermediate"
        ));

        // Question 34
        list.add(new QuestionModel(
                "Bayangkan kamu seorang fisikawan yang ditugaskan untuk mengoptimalkan desain vegetable washer yang menggunakan mekanisme drum berputar. Dengan jari-jari (r) sebesar 0.1 m, tujuanmu adalah menentukan kecepatan sudut maksimum (ω) yang aman agar percepatan sentripetal (a) tidak melebihi 50 m/s² selama pengoperasian. Pertimbangkan bahwa drum harus berputar dengan lancar dan aman agar sayuran tidak rusak. Berapa kecepatan sudut maksimum (ω) yang kamu rekomendasikan?",
                Arrays.asList(R.drawable.intermediate_35), "Intermediate"
        ));

        // Question 35
        list.add(new QuestionModel(
                "Fifi mengamati vegetable washer dan melihat gerakan rotasi dari bilah-bilah bagian dalamnya. Ia memahami bahwa bilah-bilah tersebut mengalami gerakan melingkar karena adanya percepatan sentripetal (a). Jika ia mengetahui jari-jari (r) washer adalah 0.1 m dan bisa mengukur waktu satu putaran penuh, bisakah ia menentukan percepatan sentripetal dari bilah-bilah tersebut?",
                Arrays.asList(R.drawable.intermediate_36), "Intermediate"
        ));

        // Question 36
        list.add(new QuestionModel(
                "Seorang siswa sedang mempelajari perilaku vegetable washer dan mencatat bagaimana kecepatan sudut (ω) dan percepatan sentripetal (a) berubah seiring waktu. Ia mengamati bahwa ω dan a mulai dari nol, meningkat secara bertahap, mencapai puncaknya pada 10.89 detik, lalu mulai menurun. Dengan kecepatan sudut maksimum sebesar 14.55 rad/s dan percepatan sentripetal sebesar 18.32 m/s² pada waktu yang sama, ia menduga bahwa a bergantung pada kuadrat dari ω. Bisakah kamu membantu memverifikasi dugaan ini dengan menginterpretasi tren data di sekitar fase puncak dan stabil?",
                Arrays.asList(R.drawable.intermediate_37), "Intermediate"
        ));

        // Question 37
        list.add(new QuestionModel(
                "Kamu sedang menganalisis mekanisme putaran dari vegetable washer untuk proyek fisikamu. Data yang kamu catat menunjukkan bahwa ω meningkat secara bertahap dan stabil di sekitar 13.16 rad/s, sementara a juga meningkat lalu stabil di sekitar 16.71 m/s². Berdasarkan pola ini, bisakah kamu menjelaskan hubungan antara ω dan a, serta memprediksi apa yang akan terjadi pada a jika washer berputar lebih cepat? Fokuskan analisis hanya pada periode stabil dengan mengabaikan 5 data awal dan akhir.",
                Arrays.asList(R.drawable.intermediate_38), "Intermediate"
        ));

        // Question 38
        list.add(new QuestionModel(
                "Selama fase stabil dari detik ke-6 hingga ke-20 dalam eksperimen vegetable washer, kecepatan sudut (ω) tetap sekitar 13 rad/s dan jari-jari (r) adalah 0.1 m. Berdasarkan periode gerakan stabil ini, bagaimana pengaruh peningkatan jari-jari (r) terhadap percepatan sentripetal (a) jika ω tetap konstan?",
                Arrays.asList(R.drawable.intermediate_39), "Intermediate"
        ));

        // Question 39
        list.add(new QuestionModel(
                "Kamu sedang mengamati vegetable washer dengan jari-jari (r) sebesar 0.1 m. Selama periode dari detik ke-15 hingga ke-20, kecepatan sudut (ω) tetap relatif stabil, berfluktuasi sedikit antara 13 hingga 14 rad/s. Dengan perilaku ω yang stabil ini, bagaimana kamu memperkirakan perilaku percepatan sentripetal (a) selama periode ini?",
                Arrays.asList(R.drawable.intermediate_40), "Intermediate"
        ));

        // Question 40
        list.add(new QuestionModel(
                "Kamu adalah mahasiswa fisika yang sedang melakukan eksperimen dengan sistem outdoor fitness double wheel yang memiliki jari-jari (r) sebesar 0.43 m. Berdasarkan data yang tercatat, kecepatan sudut maksimum (ω) mencapai 7.74 rad/s dan percepatan sentripetal maksimum (a) mencapai 27.12 m/s² pada waktu yang sama. Awalnya, ω adalah 0 rad/s dan a adalah 0.01 m/s². Seiring waktu, ω dan a meningkat secara konsisten. Menjelang akhir eksperimen, ω stabil di sekitar 5.04 rad/s dan a tercatat sebesar 13.97 m/s². Berdasarkan pengamatan ini, jelaskan bagaimana perubahan ω memengaruhi a dan mengapa hubungan ini mengikuti pola fisika tertentu.",
                Arrays.asList(R.drawable.intermediate_41_1, R.drawable.intermediate_41_2), "Intermediate"
        ));

        // Question 41
        list.add(new QuestionModel(
                "Selama eksperimen, seorang siswa mengamati sepasang outdoor fitness double wheel dengan jari-jari roda (r) sebesar 0.43 m. Siswa tersebut mencatat ω dan a seiring waktu, dan melihat dua keadaan stabil pada ω = 4.29 rad/s (a = 4.63 m/s²) dan ω = 6.18 rad/s (a = 15.3 m/s²), serta puncaknya pada ω = 7.74 rad/s (a = 27.12 m/s²). Jika siswa tersebut diminta untuk merancang eksperimen lanjutan untuk meneliti hubungan antara ω, r, dan a, faktor kunci apa yang harus mereka perhatikan agar data yang dikumpulkan dapat diandalkan dan akurat?",
                Arrays.asList(R.drawable.intermediate_42_1, R.drawable.intermediate_42_2), "Intermediate"
        ));

        // Question 42
        list.add(new QuestionModel(
                "Seorang siswa mengamati rotating display plate dengan jari-jari (r) sebesar 0.14 m. Siswa tersebut mencatat kecepatan sudut (ω) dan percepatan sentripetal (a) dari waktu ke waktu. Jelaskan mengapa percepatan sentripetal meningkat selama interval waktu dari 3.08 detik hingga 8.34 detik.",
                Arrays.asList(R.drawable.intermediate_43_1, R.drawable.intermediate_43_2), "Intermediate"
        ));

        // Question 43
        list.add(new QuestionModel(
                "Kamu mengamati rotating display plate dengan jari-jari (r) sebesar 0.14 m. Kamu mencatat ω dan a dari waktu ke waktu. Bagaimana jika jari-jari diubah menjadi 0.10 m (bukan 0.14 m), bagaimana hal itu akan memengaruhi percepatan yang terukur selama fase stabil (ω ≈ 15.5 rad/s)? Hitung dan bandingkan nilainya",
                Arrays.asList(R.drawable.intermediate_44_1, R.drawable.intermediate_44_2), "Intermediate"
        ));

        // Question 44
        list.add(new QuestionModel(
                "Berdasarkan pengaturan eksperimen dan data gerak rotasi dari vegetable washer dengan jari-jari 0.1 m, bagaimana perubahan kecepatan sudut (ω) dari waktu ke waktu memengaruhi percepatan sentripetal (a)? Pada interval waktu mana sistem mempertahankan gerakan stabil, dan apa arti hal tersebut terhadap perilaku rotasi washer?",
                Arrays.asList(R.drawable.intermediate_45_1, R.drawable.intermediate_45_2), "Intermediate"
        ));

        // Question 45
        list.add(new QuestionModel(
                "Selama periode stabil dari 0 hingga 1.5 detik, outdoor fitness double wheel mempertahankan kecepatan sudut (ω) konstan dengan jari-jari (r) sebesar 0.43 m. Jika kecepatan sudut (ω) meningkat selama periode fluktuasi dari 1.5 hingga 5 detik, bagaimana perubahan ini akan mempengaruhi percepatan sentripetal (a) dari roda tersebut?",
                Arrays.asList(R.drawable.intermediate_46_1, R.drawable.intermediate_46_2), "Intermediate"
        ));

        // Question 46
        list.add(new QuestionModel(
                "Jika seorang siswa mengamati outdoor fitness double wheel selama 30 detik dan mencatat kecepatan sudut (ω) pada berbagai interval waktu, dia menyadari bahwa ω stabil sekitar 3 rad/s antara 5 hingga 6 detik, dan sekitar 6 rad/s antara 11 hingga 12 detik. Jika jari-jari (r) roda tersebut adalah 0.43 m, dapatkah siswa tersebut menyimpulkan bahwa percepatan sentripetal (a) lebih besar saat ω = 6 rad/s dibandingkan saat ω = 3 rad/s?",
                Arrays.asList(R.drawable.intermediate_47_1, R.drawable.intermediate_47_2), "Intermediate"
        ));

        // Question 47
        list.add(new QuestionModel(
                "Jika jari-jari vegetable washer adalah 0.1 m, bagaimana cara menghitung percepatan sentripetal (a) pada saat kecepatan sudut (ω) mencapai puncaknya sekitar 14 rad/s menggunakan hubungan antara a, r, dan ω? Selain itu, fase gerak apa yang diwakili oleh puncak ini, dan bagaimana pengaruhnya terhadap stabilitas washer?",
                Arrays.asList(R.drawable.intermediate_48_1, R.drawable.intermediate_48_2), "Intermediate"
        ));

        // Question 48
        list.add(new QuestionModel(
                "Menggunakan data kecepatan sudut (ω) yang ditampilkan dalam grafik dan jari-jari vegetable washer yang berputar (0.1 m), bagaimana kamu menghitung percepatan sentripetal (a) selama fase stabil? Identifikasi interval waktu di mana ω paling konsisten, dan jelaskan mengapa fase ini penting untuk menjaga gerakan yang seragam.",
                Arrays.asList(R.drawable.intermediate_49_1, R.drawable.intermediate_49_2), "Intermediate"
        ));

        // Question 49
        list.add(new QuestionModel(
                "Mengingat bahwa kipas angin memiliki jari-jari sebesar 0.17 meter, gunakan data kecepatan sudut (ω) dari grafik untuk menghitung percepatan sentripetal (a) selama fase stabil. Pada interval waktu manakah kecepatan sudut paling stabil, dan bagaimana hal ini akan mempengaruhi gaya sentripetal yang bekerja pada bilah kipas?",
                Arrays.asList(R.drawable.intermediate_50_1, R.drawable.intermediate_50_1), "Intermediate"
        ));

        // Question 50
        list.add(new QuestionModel(
                "Berdasarkan data dari eksperimen outdoor fitness double wheel, kamu diminta untuk menentukan jari-jari (r) pada saat kecepatan sudut (ω) mencapai puncaknya. Pada momen ini, percepatan sentripetal (a) adalah 27.12 m/s² dan ω adalah 7.74 rad/s. Menggunakan hubungan yang diketahui antara percepatan sentripetal, kecepatan sudut, dan jari-jari, perkirakan nilai r.",
                Arrays.asList(R.drawable.intermediate_51_1, R.drawable.intermediate_51_2), "Intermediate"
        ));

        // Question 51
        list.add(new QuestionModel(
                "Luke, seorang siswa sekolah menengah, telah mengamati alat outdoor fitness double wheel. Ia mencatat bahwa jari-jari (r) dari roda adalah 0.43 m. Selama pengamatannya, kecepatan sudut (ω) meningkat dari 0 rad/s menjadi 4.58 rad/s, dan percepatan sentripetal (a) yang sesuai meningkat dari 0.01 m/s² menjadi 9.8 m/s². Kecepatan sudut maksimum sebesar 7.74 rad/s dan percepatan sentripetal puncak sebesar 27.12 m/s² keduanya tercatat pada 10.16 detik. Sekarang, Luke penasaran: jika kecepatan sudut (ω) mencapai 10 rad/s, berapa kira-kira percepatan sentripetal (a) dari roda tersebut?",
                Arrays.asList(R.drawable.intermediate_52_1, R.drawable.intermediate_52_2), "Intermediate"
        ));

        // Question 52
        list.add(new QuestionModel(
                "Menggunakan data dari vegetable washer dengan jari-jari 0.1 m, hitung percepatan sentripetal (a) pada 10.89 detik saat kecepatan sudut (ω) mencapai sekitar 13.99 rad/s. Bagaimana perubahan percepatan ini di antara tiga fase (meningkat, stabil, menurun), dan apa implikasinya terhadap kinerja washer dari waktu ke waktu?",
                Arrays.asList(R.drawable.intermediate_53_1, R.drawable.intermediate_53_2), "Intermediate"
        ));

        // Question 53
        list.add(new QuestionModel(
                "Menggunakan data kecepatan sudut (ω) dan percepatan sentripetal (a) dari vegetable washer, hitung percepatan sentripetal (a) pada waktu 10.89 detik menggunakan jari-jari 0.1 m. Kemudian bandingkan nilai yang dihitung ini dengan nilai percepatan aktual yang tercatat.",
                Arrays.asList(R.drawable.intermediate_54_1, R.drawable.intermediate_54_2), "Intermediate"
        ));

        // Question 54
        list.add(new QuestionModel(
                "Mengingat jari-jari dari rotating display plate adalah 0.14 m, identifikasi waktu saat percepatan sentripetal (a) paling tinggi dan verifikasi apakah waktu tersebut juga merupakan saat kecepatan sudut (ω) paling tinggi. Bagaimana pengamatan ini mendukung hubungan antara a dan ω?",
                Arrays.asList(R.drawable.intermediate_55_1, R.drawable.intermediate_55_2), "Intermediate"
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
