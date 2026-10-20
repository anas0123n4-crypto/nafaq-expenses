package com.nafaq.expenses.data

import com.nafaq.expenses.data.db.CategoryEntity

object CategoryCatalog {

    val DEFAULTS: List<CategoryEntity> = listOf(
        CategoryEntity("food", "طعام ومواد غذائية", "#C2703D", "🍚", 0),
        CategoryEntity("produce", "خضار وفواكه", "#5E8C4A", "🥬", 1),
        CategoryEntity("meat", "لحوم ودواجن", "#A64B4B", "🍗", 2),
        CategoryEntity("sweets", "حلويات", "#C98BB0", "🍮", 3),
        CategoryEntity("readyfood", "أكل جاهز ومطاعم", "#D98E32", "🥙", 4),
        CategoryEntity("drinks", "مشروبات", "#4E8FA8", "🧃", 5),
        CategoryEntity("cleaning", "منظفات", "#7C6BC4", "🧴", 6),
        CategoryEntity("household", "أدوات منزلية", "#7E9BA8", "🏠", 7),
        CategoryEntity("furniture", "أثاث", "#8B6F47", "🪑", 8),
        CategoryEntity("clothes", "ملابس", "#B06BA8", "👕", 9),
        CategoryEntity("health", "صحة ودواء", "#57A08C", "💊", 10),
        CategoryEntity("transport", "مواصلات ووقود", "#5C6BC0", "🚗", 11),
        CategoryEntity("maintenance", "صيانة وإصلاحات", "#8A7B66", "🔧", 12),
        CategoryEntity("fun", "ترفيه", "#D76D8E", "🎈", 13),
        CategoryEntity("education", "تعليم", "#4F8FCB", "📚", 14),
        CategoryEntity("other", "أخرى", "#9A9186", "📦", 15)
    )

    val PALETTE: List<String> = listOf(
        "#C2703D", "#5E8C4A", "#A64B4B", "#C98BB0", "#D98E32", "#4E8FA8",
        "#7C6BC4", "#7E9BA8", "#8B6F47", "#B06BA8", "#57A08C", "#5C6BC0",
        "#8A7B66", "#D76D8E", "#4F8FCB", "#9A9186"
    )
}
