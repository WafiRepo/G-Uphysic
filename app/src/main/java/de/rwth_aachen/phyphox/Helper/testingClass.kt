package de.rwth_aachen.phyphox.Helper

import de.rwth_aachen.phyphox.R
import de.rwth_aachen.phyphox.model.QuestionModel

class testingClass {
    val questions = listOf(
        // Questions 1-20 (assuming these were already created)
        // ...

        // Question 21
        QuestionModel(
            "Sebuah waist trainer berputar dengan kecepatan sudut tetap (ω) dalam kondisi stabil. Jari-jarinya (r) adalah 0,09 m, dan antara detik ke-7 hingga ke-9, percepatan sentripetal (a) teramati tetap sekitar 15 m/s². Mengingat periode yang stabil ini, bagaimana kecepatan sudut waist trainer (ω) memengaruhi percepatan sentripetal (a) yang diamati? Bisakah kamu menjelaskan hubungan antara variabel-variabel ini dalam selang waktu tersebut?",
            listOf(R.drawable.easy_21), "Easy"
        ),

        // Question 22
        QuestionModel(
            "Dalam rentang waktu antara detik ke-7 hingga ke-10, percepatan sentripetal (a) dari waist trainer teramati stabil sekitar 15 m/s². Apa yang bisa dikatakan tentang perilaku kecepatan sudut (ω) selama periode ini?",
            listOf(R.drawable.easy_22),"Easy"
        ),

        // Question 23
        QuestionModel(
            "Seorang siswa mengamati gerakan waist trainer. Waist trainer tersebut bergerak dalam lintasan melingkar dengan jari-jari (r) sebesar 0,09 m. Jika siswa tersebut mengukur kecepatan sudut (ω) dari gerakan waist trainer, dapatkah kamu jelaskan arah dari kecepatan sudut?",
            listOf(R.drawable.easy_23),"Easy"
        ),

        // Question 24
        QuestionModel(
            "Jika Sarah sedang mempelajari mekanisme waist trainer, dan ia mengukur jari-jari (r) dari pusat kipas ke tepi waist trainer sebesar 0,09 m. Ia mengamati bahwa alat tersebut menyelesaikan satu putaran penuh dalam 0,5 detik. Bisakah kamu membantu Sarah menunjukkan rumus hubungan antara variabel-variabel tersebut?",
            listOf(R.drawable.easy_24),"Easy"
        ),

        // Question 25
        QuestionModel(
            "Berdasarkan pola yang diamati dalam eksperimen waist trainer, di mana kecepatan sudut (ω) dan percepatan sentripetal (a) meningkat dan menurun bersama-sama, bagaimana hal ini mencerminkan prinsip bahwa percepatan sentripetal berbanding lurus dengan kuadrat dari kecepatan sudut?",
            listOf(R.drawable.easy_25),"Easy"
        ),

        // Continue with questions 26-60 in the same pattern
        // ...

        // Question 60
        QuestionModel(
            "Ini adalah data grafik dan tabel dari piring pajangan berputar. Selama fase mana kecepatan sudut diperkirakan tetap stabil?",
            listOf(R.drawable.easy_60)
        )
    )
}
