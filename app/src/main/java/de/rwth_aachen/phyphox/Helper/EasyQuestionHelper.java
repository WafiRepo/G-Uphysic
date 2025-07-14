package de.rwth_aachen.phyphox.Helper;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import de.rwth_aachen.phyphox.R;
import de.rwth_aachen.phyphox.model.QuestionModel;

public class EasyQuestionHelper {
    public static List<QuestionModel> generateQuestionList() {
        List<QuestionModel> list = new ArrayList<>();

        list.add(new QuestionModel(
                "Sebuah waist trainer berputar dengan kecepatan sudut tetap (ω) dalam kondisi stabil. Jari-jarinya (r) adalah 0,09 m, dan antara detik ke-7 hingga ke-9, percepatan sentripetal (a) teramati tetap sekitar 15 m/s². Mengingat periode yang stabil ini, bagaimana kecepatan sudut waist trainer (ω) memengaruhi percepatan sentripetal (a) yang diamati? Bisakah kamu menjelaskan hubungan antara variabel-variabel ini dalam selang waktu tersebut?",
                Arrays.asList(R.drawable.easy_1),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Dalam rentang waktu antara detik ke-7 hingga ke-10, percepatan sentripetal (a) dari waist trainer teramati stabil sekitar 15 m/s². Apa yang bisa dikatakan tentang perilaku kecepatan sudut (ω) selama periode ini?",
                Arrays.asList(R.drawable.easy_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Seorang siswa mengamati gerakan waist trainer. Waist trainer tersebut bergerak dalam lintasan melingkar dengan jari-jari (r) sebesar 0,09 m. Jika siswa tersebut mengukur kecepatan sudut (ω) dari gerakan waist trainer, dapatkah kamu jelaskan arah dari kecepatan sudut?",
                Arrays.asList(R.drawable.easy_3),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Jika Sarah sedang mempelajari mekanisme waist trainer, dan ia mengukur jari-jari (r) dari pusat kipas ke tepi waist trainer sebesar 0,09 m. Ia mengamati bahwa alat tersebut menyelesaikan satu putaran penuh dalam 0,5 detik. Bisakah kamu membantu Sarah menunjukkan rumus hubungan antara variabel-variabel tersebut?",
                Arrays.asList(R.drawable.easy_4),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan pola yang diamati dalam eksperimen waist trainer, di mana kecepatan sudut (ω) dan percepatan sentripetal (a) meningkat dan menurun bersama-sama, bagaimana hal ini mencerminkan prinsip bahwa percepatan sentripetal berbanding lurus dengan kuadrat dari kecepatan sudut?",
                Arrays.asList(R.drawable.easy_5),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bisakah kamu mengidentifikasi fase peningkatan, stabil, dan penurunan dalam tabel eksperimen waist trainer dengan mengamati perubahan kecepatan sudut dan percepatan sentripetal terhadap waktu?",
                Arrays.asList(R.drawable.easy_6),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Sebuah roda sedang berputar dengan kecepatan tetap. Jika seorang siswa meningkatkan kecepatan sudut (ω) dari roda tersebut, apa dampaknya terhadap percepatan sentripetal (a)? Apakah percepatan sentripetal akan meningkat, menurun, atau tetap sama? Jari-jari roda adalah 0,25 m.",
                Arrays.asList(R.drawable.easy_7),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Kamu sedang mengamati sebuah roda dengan jari-jari 0,25 m. Saat roda berputar pada kecepatan tertingginya, ujung roda bergerak dengan kecepatan sudut (ω). Dapatkah kamu menentukan percepatan sentripetal (a) dari ujung roda tersebut? Harap nyatakan jawabanmu dalam bentuk ω.",
                Arrays.asList(R.drawable.easy_8),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Selama periode stabil antara detik ke-13 hingga ke-17 dalam grafik, jika diketahui jari-jari roda adalah 0,25 m, dapatkah kamu memperkirakan apakah kecepatan sudut (ω) roda tersebut tinggi atau rendah berdasarkan percepatan sentripetal (a) yang relatif stabil sekitar 7 m/s²?",
                Arrays.asList(R.drawable.easy_9),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Selama periode stabil dari detik ke-13 hingga ke-17, percepatan sentripetal (a) dari kipas listrik teramati tetap sekitar 7 m/s². Apa yang bisa disimpulkan mengenai kecepatan sudut (ω) dari roda tersebut selama periode ini?",
                Arrays.asList(R.drawable.easy_10),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan analisis data yang diberikan, dapatkah kamu menjelaskan mengapa mungkin terjadi fluktuasi dalam korelasi antara percepatan sentripetal (a) dan kecepatan sudut (ω) dari roda, terutama selama rentang waktu 5,22 s hingga 6,7 s?",
                Arrays.asList(R.drawable.easy_11),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan observasi data, dapatkah kamu menjelaskan bagaimana perubahan jari-jari roda memengaruhi percepatan sentripetal (a) dan kecepatan sudut (ω), serta bagaimana kedua variabel ini saling berkaitan selama berbagai fase gerakan?",
                Arrays.asList(R.drawable.easy_12),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bisakah kamu memberi label pada fase-fase dalam tabel data eksperimen waist trainer, menunjukkan mana yang merupakan fase peningkatan, stabil, dan penurunan?",
                Arrays.asList(R.drawable.easy_13),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan gambar outdoor fitness double wheel dengan parameter yang diberikan, dapatkah kamu menjelaskan peran dari jari-jari (r), kecepatan sudut (ω), dan percepatan sentripetal (a) dalam fungsionalitas roda tersebut? Jelaskan juga bagaimana ketiga variabel ini saling berinteraksi dalam konteks gerak rotasi.",
                Arrays.asList(R.drawable.easy_14),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Kamu sedang mengamati outdoor fitness double wheel. Roda tersebut berputar dalam lintasan melingkar. Jarak dari pusat roda ke tepi bilah adalah jari-jari (r) 0,43 m dan kecepatan rotasinya adalah kecepatan sudut (ω). Dengan mempertimbangkan istilah-istilah ini, bisakah kamu menjelaskan bagaimana percepatan sentripetal (a) dari bilah dipengaruhi oleh jari-jari dan kecepatan sudut?",
                Arrays.asList(R.drawable.easy_15),
                "Easy"
        ));

        list.add(new QuestionModel(
                "John sedang mengamati alat outdoor fitness double wheel. Ia melihat bahwa saat kecepatan sudut (ω) roda meningkat, percepatan sentripetal (a) juga ikut meningkat. Jika jari-jari roda adalah 0,43 meter, bagaimana hubungan antara ω dan a?",
                Arrays.asList(R.drawable.easy_16),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Rina, seorang mahasiswa fisika, sedang menganalisis data dari eksperimen outdoor fitness double wheel dengan jari-jari bilah 0,43 m. Rina melihat bahwa saat alat outdoor fitness double wheel berputar lebih cepat, percepatan sentripetal juga meningkat. Namun, terkadang a tetap stabil meskipun ω berubah. Apa yang bisa memengaruhi hubungan ini?",
                Arrays.asList(R.drawable.easy_17),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Selama periode stabil dari detik ke-10 hingga ke-17, kecepatan sudut (ω) dari outdoor fitness double wheel tetap stabil sekitar 6 hingga 7 rad/s. Jika jari-jari (r) dari roda adalah 0,43 m, bagaimana percepatan sentripetal (a) akan terpengaruh selama periode ini?",
                Arrays.asList(R.drawable.easy_18),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Selama periode stabil dari detik ke-30 hingga ke-35, outdoor fitness double wheel memiliki kecepatan sudut (ω) sekitar 1 rad/s dan jari-jari (r) sebesar 0,43 m. Apa yang akan terjadi pada percepatan sentripetal (a) jika kecepatan sudut (ω) menjadi dua kali lipat sementara jari-jari (r) tetap sama?",
                Arrays.asList(R.drawable.easy_19),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Kamu sedang mengamati kipas listrik. Bilah-bilah kipas berputar dalam lintasan melingkar. Jarak dari pusat kipas ke ujung bilah adalah jari-jari (r), dan kecepatan rotasi bilah adalah kecepatan sudut (ω). Dengan mempertimbangkan istilah-istilah ini, dapatkah kamu menjelaskan bagaimana percepatan sentripetal (a) dari bilah kipas dipengaruhi oleh jari-jari dan kecepatan sudut?",
                Arrays.asList(R.drawable.easy_20),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Kamu sedang mengamati kipas listrik dengan jari-jari 0,17 m. Saat kipas berputar pada kecepatan tertingginya, ujung bilah bergerak dengan kecepatan sudut (ω). Dapatkah kamu menentukan percepatan sentripetal (a) dari ujung bilah tersebut? Harap nyatakan jawabanmu dalam bentuk ω.",
                Arrays.asList(R.drawable.easy_21),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Dapatkah kamu mengidentifikasi fase-fase berbeda dalam tabel yang diberikan, dengan menyoroti periode di mana kecepatan sudut (ω) dan percepatan sentripetal (a) meningkat, stabil, atau menurun untuk kipas listrik?",
                Arrays.asList(R.drawable.easy_22),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Dapatkah kamu memberi label pada fase-fase dalam tabel yang diberikan, menunjukkan mana yang merupakan fase peningkatan, fase stabil, dan fase penurunan dari rotasi kipas listrik?",
                Arrays.asList(R.drawable.easy_23),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diberikan roda sepeda dengan jari-jari (r) 0,2 meter dan pemahaman tentang kecepatan sudut (ω), dapatkah kamu menjelaskan bagaimana perubahan kecepatan sudut memengaruhi percepatan sentripetal (a) dari roda?",
                Arrays.asList(R.drawable.easy_24),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Sebuah roda sepeda dengan jari-jari (r) 0,2 meter diamati selama periode waktu tertentu. Berdasarkan grafik yang diberikan dan analisis data eksperimen, dapatkah kamu menjelaskan bagaimana percepatan sentripetal (a) akan berperilaku selama periode yang berbeda saat kecepatan sudut (ω) baik stabil maupun berfluktuasi?",
                Arrays.asList(R.drawable.easy_25),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Mengacu pada grafik dan observasi roda sepeda dengan jari-jari (r) 0,2 meter, dapatkah kamu merangkum hubungan antara kecepatan sudut (ω) dan percepatan sentripetal (a) selama periode stabil dan periode fluktuasi?",
                Arrays.asList(R.drawable.easy_26),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Gambar menunjukkan mesin pencuci sayuran saat beroperasi. Bisakah kamu mengidentifikasi dan menjelaskan komponen dan konsep kunci dalam konteks fungsi alat tersebut, khususnya percepatan sentripetal (a), jari-jari (r), dan kecepatan sudut (ω)?",
                Arrays.asList(R.drawable.easy_27),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bisakah kamu menjelaskan bagaimana jari-jari (r) bilah mesin pencuci sayuran memengaruhi percepatan sentripetal (a) dan kecepatan sudut (ω)?",
                Arrays.asList(R.drawable.easy_28),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diketahui mesin pencuci sayuran dengan jari-jari (r) 0,1 m, bagaimana kamu akan menggambarkan hubungan antara kecepatan sudut (ω) dan percepatan sentripetal (a) saat alat mulai berputar, mencapai kecepatan puncak, dan akhirnya melambat?",
                Arrays.asList(R.drawable.easy_29),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diketahui mesin pencuci sayuran dengan jari-jari (r) 0,1 m, dan mempertimbangkan pergerakannya dari keadaan diam hingga kecepatan maksimum, bagaimana percepatan sentripetal (a) dan kecepatan sudut (ω) berubah seiring waktu? Pola seperti apa yang mungkin kamu amati dalam (a) dan (ω) jika alat kemudian melambat?",
                Arrays.asList(R.drawable.easy_30),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Dalam eksperimen mesin pencuci sayuran, bagaimana respons percepatan sentripetal (a) selama peningkatan awal kecepatan sudut (ω) dari 0s hingga 5s, periode stabil dari 10s hingga 20s saat ω tetap sekitar 14 rad/s, dan penurunan tajam dari 24s hingga 30s saat ω menurun mendekati nol?",
                Arrays.asList(R.drawable.easy_31),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diberikan data grafik dari mesin pencuci sayuran dengan jari-jari (r) 0,1 m, grafik menunjukkan bagaimana kecepatan sudut (ω) berubah seiring waktu dan dampaknya terhadap percepatan sentripetal (a). Grafik memperlihatkan bahwa dari 0 hingga sekitar 5 detik, ω meningkat cepat, yang menyebabkan peningkatan proporsional dalam a; dari 10 hingga 20 detik, ω stabil sekitar 14 rad/s, menghasilkan a yang stabil; dan setelah 25 detik, ω menurun tajam, menyebabkan a menurun. Bisakah kamu merangkum bagaimana fase-fase berbeda dari kecepatan sudut berkaitan dengan perubahan dalam percepatan sentripetal?",
                Arrays.asList(R.drawable.easy_32),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan data yang diamati dari mesin pencuci sayuran dengan jari-jari 0,1 m, dapatkah kamu menjelaskan bagaimana perubahan percepatan sentripetal berkaitan dengan kecepatan sudut selama periode operasi yang berbeda?",
                Arrays.asList(R.drawable.easy_33),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diketahui mesin pencuci sayuran dengan jari-jari 0,1 meter dan perubahan kecepatan sudut yang diamati dari waktu ke waktu seperti yang ditunjukkan dalam grafik, bagaimana perilaku percepatan sentripetal selama periode peningkatan, stabil, dan penurunan sepanjang eksperimen 30 detik?",
                Arrays.asList(R.drawable.easy_34),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Gambar menunjukkan mesin pencuci sayuran dengan jari-jari (r) 0,1 meter. Dalam konteks gerakan rotasinya, dapatkah kamu menjelaskan arti dari percepatan sentripetal (a) untuk komponen-komponen yang berputar di dalam alat tersebut?",
                Arrays.asList(R.drawable.easy_35),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Gambar menunjukkan mesin pencuci sayuran dengan jari-jari (r) 0,1 meter. Dalam konteks gerakan rotasinya, dapatkah kamu menjelaskan bagaimana jari-jari (r) berhubungan dengan percepatan ke dalam yang dialami oleh bagian yang berputar?",
                Arrays.asList(R.drawable.easy_36),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan gambar rotating display plate, dapatkah kamu menjelaskan bagaimana jari-jari (r), percepatan sentripetal (a), dan kecepatan sudut (ω) ditampilkan secara visual dan bagaimana mereka saling terkait dalam mempertahankan gerak melingkar plate tersebut?",
                Arrays.asList(R.drawable.easy_37),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diberikan gambar rotating display plate, dapatkah kamu mengidentifikasi dan menjelaskan fungsi dari percepatan sentripetal (a), jari-jari (r), dan kecepatan sudut (ω) berdasarkan anotasi visual? Harap jelaskan bagaimana masing-masing elemen berkontribusi terhadap gerakan melingkar yang diamati dalam sistem tersebut.",
                Arrays.asList(R.drawable.easy_38),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan grafik yang menunjukkan kecepatan sudut dan percepatan sentripetal dari rotating display plate, dapatkah kamu mengidentifikasi interval waktu spesifik di mana peningkatan paling signifikan dalam percepatan sentripetal terjadi?",
                Arrays.asList(R.drawable.easy_39),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Sebuah rotating display plate dengan jari-jari 0,14 m menunjukkan perubahan kecepatan sudut pada berbagai interval waktu. Antara detik ke-2 hingga ke-7, kecepatan sudut meningkat cepat, sedangkan antara detik ke-7 hingga ke-16 tetap stabil. Bagaimana perubahan spesifik dalam kecepatan sudut ini memengaruhi percepatan sentripetal dari piring tersebut selama periode ini?",
                Arrays.asList(R.drawable.easy_40),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diberikan grafik dari rotating display plate dengan jari-jari 0,14 m, dapatkah kamu menjelaskan bagaimana percepatan sentripetal berubah dari waktu ke waktu dan bagaimana perubahan tersebut berkaitan dengan tren kecepatan sudut yang diamati dalam berbagai segmen waktu?",
                Arrays.asList(R.drawable.easy_41),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan grafik data rotating display plate dengan jari-jari 0,14 m, dapatkah kamu mengidentifikasi berbagai fase percepatan sentripetal dari waktu ke waktu—seperti fase stabil dan peningkatan selama interval 0 hingga 30 detik?",
                Arrays.asList(R.drawable.easy_42),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Rotating display plate dengan jari-jari (r) 0,14 m, serta perubahan kecepatan sudut (ω) dan percepatan sentripetal (a) dari waktu ke waktu, bisakah kamu menjelaskan bagaimana fase stabil dan puncak dari sistem tersebut menggambarkan hubungan antara kecepatan sudut dan percepatan sentripetal?",
                Arrays.asList(R.drawable.easy_43),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan data yang diamati dari rotating display plate dengan jari-jari tetap, jelaskan bagaimana percepatan sentripetal (a) berubah saat kecepatan sudut (ω) meningkat.",
                Arrays.asList(R.drawable.easy_44),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Kamu sedang mengamati kipas listrik dengan jari-jari 0,13 m. Saat kipas berputar pada kecepatan tertingginya, ujung bilahnya bergerak dengan kecepatan sudut (ω). Dapatkah kamu menentukan percepatan sentripetal (a) dari ujung bilah tersebut? Harap nyatakan jawabanmu dalam bentuk ω.",
                Arrays.asList(R.drawable.easy_45),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Gambar menunjukkan sebuah sepeda dengan jari-jari (r) sebesar 0,2 m. Pada awal eksperimen, kecepatan sudut (ω) stabil pada 0,08 rad/s, menghasilkan percepatan sentripetal (a) sebesar 0,12 m/s². Pada akhir eksperimen, roda mencapai kecepatan sudut stabil sebesar 7,44 rad/s, dengan percepatan sentripetal 12,97 m/s². Ringkaslah hubungan antara jari-jari, kecepatan sudut, dan percepatan sentripetal seperti yang ditunjukkan dalam eksperimen ini.",
                Arrays.asList(R.drawable.easy_46),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan data eksperimen dan gambar roda sepeda, jelaskan bagaimana perubahan percepatan sentripetal (a) dan kecepatan sudut (ω) terpengaruh ketika jari-jari roda (r) ditingkatkan?",
                Arrays.asList(R.drawable.easy_47_1, R.drawable.easy_47_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan perubahan kecepatan sudut dan gambar anotasi dari piring pajangan berputar, dapatkah kamu menjelaskan bagaimana arah dan besar percepatan sentripetal (a) terpengaruh selama berbagai fase gerakan?",
                Arrays.asList(R.drawable.easy_48_1, R.drawable.easy_48_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bagaimana perubahan kecepatan sudut yang diamati antara detik ke-3 hingga ke-30 memengaruhi arah dan besar percepatan sentripetal pada piring pajangan berputar seperti yang digambarkan dalam gambar?",
                Arrays.asList(R.drawable.easy_49_1, R.drawable.easy_49_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan gambar mesin pencuci sayuran yang menunjukkan jari-jari (r), kecepatan sudut (ω), dan percepatan sentripetal (a), bagaimana ketiga hal itu saling berhubungan saat mesin mulai berputar hingga berputar stabil?",
                Arrays.asList(R.drawable.easy_50_1, R.drawable.easy_50_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Gambar menunjukkan roda sepeda dengan jari-jari (r) 0,25 m. Menurut grafik, kecepatan sudut (ω) tetap stabil selama periode stabil 5s–7s, 11s–13s, dan 16s–18s, yang menghasilkan percepatan sentripetal (a) yang konsisten. Jika kecepatan sudut (ω) mulai menurun tajam seperti yang diamati dari detik ke-18 hingga ke-25, bagaimana hal ini akan memengaruhi percepatan sentripetal (a) dari roda sepeda, dan mengapa?",
                Arrays.asList(R.drawable.easy_51_1, R.drawable.easy_51_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Diberikan gambar roda sepeda berputar dan interpretasi grafik yang sesuai, jika roda (dengan jari-jari 0,25 m) mempertahankan kecepatan sudut (ω) stabil sebesar 6,5 rad/s dari detik ke-10 hingga ke-11, bagaimana kamu akan menggambarkan perilaku percepatan sentripetal (a) selama periode 12s hingga 14s, dengan asumsi ω tetap tidak berubah?",
                Arrays.asList(R.drawable.easy_52_1, R.drawable.easy_52_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan gambar dan data eksperimen dari mesin pencuci sayuran, bagaimana elemen-elemen yang diberi label—jari-jari (r = 0,1 m), kecepatan sudut (ω), dan percepatan sentripetal (a)—serta perubahan ω yang diamati dari 0s hingga 30s, menunjukkan hubungan antara ω dan a selama fase percepatan, stabil, dan perlambatan rotasi alat?",
                Arrays.asList(R.drawable.easy_53_1, R.drawable.easy_53_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bagaimana fitur yang dianotasi dalam gambar mesin pencuci sayuran dan tren data berbasis waktu secara bersama-sama menunjukkan hubungan antara kecepatan sudut (ω) dan percepatan sentripetal (a) selama berbagai fase gerakan?",
                Arrays.asList(R.drawable.easy_54_1, R.drawable.easy_54_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bagaimana gambar anotasi kipas—yang menyoroti jari-jari (r = 0,17 m), kecepatan sudut (ω), dan percepatan sentripetal (a)—bersama dengan data eksperimen dari 0s hingga 25s, menunjukkan perubahan dalam percepatan sentripetal selama periode kecepatan sudut yang meningkat, stabil, dan menurun?",
                Arrays.asList(R.drawable.easy_55_1, R.drawable.easy_55_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Mengacu pada tabel dan grafik, selama interval waktu manakah roda sepeda dengan jari-jari (r) tampak bergerak dalam lintasan melingkar yang stabil?",
                Arrays.asList(R.drawable.easy_56_1, R.drawable.easy_56_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Berdasarkan tabel data dan grafik, selama periode waktu manakah kecepatan sudut (ω) dari roda sepeda tampak stabil, dan bagaimana stabilitas ini memengaruhi percepatan sentripetal (a) dan jari-jari (r)?",
                Arrays.asList(R.drawable.easy_57_1, R.drawable.easy_57_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bagaimana periode awal diam, rotasi stabil, dan perlambatan akhir dari mesin pencuci sayuran—berdasarkan grafik dan nilai dalam tabel kecepatan sudut (ω) dan percepatan sentripetal (a)—dengan jelas mendukung hubungan kuadrat antara ω dan a dalam gerak melingkar?",
                Arrays.asList(R.drawable.easy_58_1, R.drawable.easy_58_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Bagaimana perubahan kecepatan sudut (ω) dan percepatan sentripetal (a) selama fase waktu yang berbeda dari eksperimen mesin pencuci sayuran mencerminkan hubungan langsung di antara keduanya, khususnya selama tahap peningkatan, stabil, dan penurunan gerakan?",
                Arrays.asList(R.drawable.easy_59_1, R.drawable.easy_59_2),
                "Easy"
        ));

        list.add(new QuestionModel(
                "Ini adalah data grafik dan tabel dari piring pajangan berputar. Selama fase mana kecepatan sudut diperkirakan tetap stabil?",
                Arrays.asList(R.drawable.easy_60_1, R.drawable.easy_60_2),
                "Easy"
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
