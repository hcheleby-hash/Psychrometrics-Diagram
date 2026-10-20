package com.psikrometri.app.model

enum class Dil(val ad: String) { TR("Türkçe"), EN("English"), ES("Español"), FR("Français"), DE("Deutsch") }

/** Etkin dil; AnaVM ayarlardan yazar. UI dil değişince key(dil) ile yeniden kurulur. */
@Volatile var dil = Dil.EN

/** Türkçe metin anahtardır; çevirisi yoksa olduğu gibi döner. */
fun String.c(): String = CEVIRI[this]?.getOrNull(dil.ordinal - 1) ?: this

// sıra: EN, ES, FR, DE
internal val CEVIRI: Map<String, Array<String>> = mapOf(
    "Psikrometri" to arrayOf("Psychrometrics", "Psicrometría", "Psychrométrie", "Psychrometrie"),
    "Menü" to arrayOf("Menu", "Menú", "Menu", "Menü"),
    "Yeni" to arrayOf("New", "Nuevo", "Nouveau", "Neu"),
    "Aç" to arrayOf("Open", "Abrir", "Ouvrir", "Öffnen"),
    "Kaydet" to arrayOf("Save", "Guardar", "Enregistrer", "Speichern"),
    "Rapor (PDF)" to arrayOf("Report (PDF)", "Informe (PDF)", "Rapport (PDF)", "Bericht (PDF)"),
    "Ayarlar" to arrayOf("Settings", "Ajustes", "Paramètres", "Einstellungen"),
    "Dil" to arrayOf("Language", "Idioma", "Langue", "Sprache"),
    "Noktalar" to arrayOf("Points", "Puntos", "Points", "Punkte"),
    "İşlemler" to arrayOf("Processes", "Procesos", "Processus", "Prozesse"),
    "Veri" to arrayOf("Data", "Datos", "Données", "Daten"),
    "Sil ve yeni" to arrayOf("Delete and new", "Borrar y nuevo", "Supprimer et nouveau", "Löschen und neu"),
    "Vazgeç" to arrayOf("Cancel", "Cancelar", "Annuler", "Abbrechen"),
    "Yeni proje" to arrayOf("New project", "Nuevo proyecto", "Nouveau projet", "Neues Projekt"),
    "Mevcut noktalar ve işlemler silinecek. Kaydetmediyseniz kaybolur." to arrayOf(
        "Current points and processes will be deleted. Unsaved work will be lost.",
        "Se borrarán los puntos y procesos actuales. Lo no guardado se perderá.",
        "Les points et processus actuels seront supprimés. Tout ce qui n'est pas enregistré sera perdu.",
        "Aktuelle Punkte und Prozesse werden gelöscht. Nicht Gespeichertes geht verloren."),
    "Tamam" to arrayOf("OK", "Aceptar", "OK", "OK"),
    "Rakım / basınç" to arrayOf("Altitude / pressure", "Altitud / presión", "Altitude / pression", "Höhe / Druck"),
    "Rakım" to arrayOf("Altitude", "Altitud", "Altitude", "Höhe"),
    "Kaydedildi" to arrayOf("Saved", "Guardado", "Enregistré", "Gespeichert"),
    "Kaydedilemedi" to arrayOf("Could not save", "No se pudo guardar", "Échec de l'enregistrement", "Speichern fehlgeschlagen"),
    "Dosya okunamadı" to arrayOf("Could not read file", "No se pudo leer el archivo", "Impossible de lire le fichier", "Datei konnte nicht gelesen werden"),
    "Raporu paylaş" to arrayOf("Share report", "Compartir informe", "Partager le rapport", "Bericht teilen"),
    "Rapor oluşturulamadı" to arrayOf("Could not create report", "No se pudo crear el informe", "Impossible de créer le rapport", "Bericht konnte nicht erstellt werden"),
    "Geçerli aralık" to arrayOf("Valid range", "Rango válido", "Plage valide", "Gültiger Bereich"),
    "Yatay kullanım" to arrayOf("Landscape mode", "Modo horizontal", "Mode paysage", "Querformat"),
    "Lütfen bir nokta ekleyin." to arrayOf("Please add a point.", "Añada un punto.", "Veuillez ajouter un point.", "Bitte fügen Sie einen Punkt hinzu."),

    // özellikler ve büyüklükler
    "Kuru termometre" to arrayOf("Dry bulb", "Bulbo seco", "Température sèche", "Trockentemperatur"),
    "Yaş termometre" to arrayOf("Wet bulb", "Bulbo húmedo", "Température humide", "Feuchttemperatur"),
    "Çiy noktası" to arrayOf("Dew point", "Punto de rocío", "Point de rosée", "Taupunkt"),
    "Bağıl nem" to arrayOf("Relative humidity", "Humedad relativa", "Humidité relative", "Relative Feuchte"),
    "Mutlak nem" to arrayOf("Humidity ratio", "Humedad absoluta", "Humidité absolue", "Feuchtegehalt"),
    "Entalpi" to arrayOf("Enthalpy", "Entalpía", "Enthalpie", "Enthalpie"),
    "Yoğunluk" to arrayOf("Density", "Densidad", "Masse volumique", "Dichte"),
    "Özgül hacim" to arrayOf("Specific volume", "Volumen específico", "Volume spécifique", "Spezifisches Volumen"),
    "Basınç" to arrayOf("Pressure", "Presión", "Pression", "Druck"),
    "Debi" to arrayOf("Airflow", "Caudal", "Débit", "Volumenstrom"),
    "Sıcaklık" to arrayOf("Temperature", "Temperatura", "Température", "Temperatur"),
    "Güç" to arrayOf("Power", "Potencia", "Puissance", "Leistung"),

    // noktalar
    "Nokta adı" to arrayOf("Point name", "Nombre del punto", "Nom du point", "Punktname"),
    "Değer" to arrayOf("Value", "Valor", "Valeur", "Wert"),
    "Ekle" to arrayOf("Add", "Añadir", "Ajouter", "Hinzufügen"),
    "Güncelle" to arrayOf("Update", "Actualizar", "Mettre à jour", "Aktualisieren"),
    "Sil" to arrayOf("Delete", "Borrar", "Supprimer", "Löschen"),
    "Nokta ekle" to arrayOf("Add point", "Añadir punto", "Ajouter un point", "Punkt hinzufügen"),
    "Debi 0'dan büyük olmalı" to arrayOf("Airflow must be greater than 0", "El caudal debe ser mayor que 0", "Le débit doit être supérieur à 0", "Volumenstrom muss größer als 0 sein"),
    "Bu ad başka bir nokta ya da işlem çıktısında kullanılıyor" to arrayOf(
        "This name is already used by another point or process output",
        "Este nombre ya lo usa otro punto o salida de proceso",
        "Ce nom est déjà utilisé par un autre point ou une sortie de processus",
        "Dieser Name wird bereits von einem anderen Punkt oder Prozessausgang verwendet"),

    // işlemler
    "İşlem" to arrayOf("Process", "Proceso", "Processus", "Prozess"),
    "Giriş" to arrayOf("Input", "Entrada", "Entrée", "Eingang"),
    "Çıkış" to arrayOf("Output", "Salida", "Sortie", "Ausgang"),
    "Sonuç" to arrayOf("Result", "Resultado", "Résultat", "Ergebnis"),
    "Başlangıç" to arrayOf("Start", "Inicio", "Départ", "Start"),
    "İkinci nokta" to arrayOf("Second point", "Segundo punto", "Deuxième point", "Zweiter Punkt"),
    "Buharlı" to arrayOf("Steam", "Vapor", "Vapeur", "Dampf"),
    "Sulu" to arrayOf("Water", "Agua", "Eau", "Wasser"),
    "Ara çizgileri göster" to arrayOf("Show intermediate lines", "Mostrar líneas intermedias", "Afficher les lignes intermédiaires", "Zwischenlinien anzeigen"),
    "Ara çizgiler" to arrayOf("Intermediate lines", "Líneas intermedias", "Lignes intermédiaires", "Zwischenlinien"),
    "Kapasite" to arrayOf("Capacity", "Capacidad", "Puissance", "Leistung"),
    "Miktar" to arrayOf("Amount", "Cantidad", "Quantité", "Menge"),
    "Toplam" to arrayOf("Total", "Total", "Total", "Gesamt"),
    "Duyulur" to arrayOf("Sensible", "Sensible", "Sensible", "Sensibel"),
    "Gizli" to arrayOf("Latent", "Latente", "Latente", "Latent"),
    "Isıtıcı" to arrayOf("Heater", "Calentador", "Batterie chaude", "Heizregister"),
    "Nem" to arrayOf("Moisture", "Humedad", "Humidité", "Feuchte"),
    "Üfleme" to arrayOf("Supply", "Impulsión", "Soufflage", "Zuluft"),
    "Noktayı göster" to arrayOf("Show point", "Mostrar punto", "Afficher le point", "Punkt anzeigen"),
    "Bağla" to arrayOf("Connect", "Conectar", "Relier", "Verbinden"),
    "Hava karışımı" to arrayOf("Air mixing", "Mezcla de aire", "Mélange d'air", "Luftmischung"),
    "Soğutma" to arrayOf("Cooling", "Enfriamiento", "Refroidissement", "Kühlung"),
    "Isıtma" to arrayOf("Heating", "Calentamiento", "Chauffage", "Heizung"),
    "Soğutma kapasitesi" to arrayOf("Cooling capacity", "Capacidad de enfriamiento", "Puissance frigorifique", "Kühlleistung"),
    "Isıtma kapasitesi" to arrayOf("Heating capacity", "Capacidad de calefacción", "Puissance calorifique", "Heizleistung"),
    "Buharlı nemlendirme" to arrayOf("Steam humidification", "Humidificación por vapor", "Humidification vapeur", "Dampfbefeuchtung"),
    "Sulu nemlendirme" to arrayOf("Water humidification", "Humidificación por agua", "Humidification par eau", "Wasserbefeuchtung"),
    "Doymaya kadar soğutma" to arrayOf("Cooling to saturation", "Enfriamiento hasta saturación", "Refroidissement jusqu'à saturation", "Kühlung bis zur Sättigung"),
    "Yoğuşma" to arrayOf("Condensation", "Condensación", "Condensation", "Kondensation"),
    "Nem alma" to arrayOf("Dehumidification", "Deshumidificación", "Déshumidification", "Entfeuchtung"),
    "Bypass faktörü" to arrayOf("Bypass factor", "Factor de bypass", "Facteur de bypass", "Bypassfaktor"),
    "Üfleme (discharge)" to arrayOf("Supply (discharge)", "Impulsión (descarga)", "Soufflage", "Zuluft (Ausblas)"),
    "Duyulur ısı faktörü" to arrayOf("Sensible heat factor", "Factor de calor sensible", "Facteur de chaleur sensible", "Sensibler Wärmefaktor"),

    // hatalar
    "Aynı özellik iki kez seçilemez" to arrayOf("The same property cannot be selected twice", "No se puede elegir la misma propiedad dos veces",
        "La même propriété ne peut pas être choisie deux fois", "Dieselbe Größe kann nicht zweimal gewählt werden"),
    "Bu değerlerle nokta bulunamadı" to arrayOf("No point found for these values", "No se encontró un punto con estos valores",
        "Aucun point trouvé pour ces valeurs", "Kein Punkt für diese Werte gefunden"),
    "Bu iki özellik birlikte noktayı belirlemiyor" to arrayOf("These two properties do not define a point", "Estas dos propiedades no definen un punto",
        "Ces deux propriétés ne définissent pas un point", "Diese beiden Größen bestimmen keinen Punkt"),
    "Sıcaklık -60…100 °C dışında" to arrayOf("Temperature outside -60…100 °C", "Temperatura fuera de -60…100 °C",
        "Température hors de -60…100 °C", "Temperatur außerhalb -60…100 °C"),
    "Nem oranı negatif" to arrayOf("Humidity ratio is negative", "La humedad absoluta es negativa", "L'humidité absolue est négative", "Feuchtegehalt ist negativ"),
    "Nokta doyma eğrisinin üstünde (RH > %100)" to arrayOf("Point above the saturation curve (RH > 100%)", "Punto por encima de la curva de saturación (HR > 100 %)",
        "Point au-dessus de la courbe de saturation (HR > 100 %)", "Punkt über der Sättigungslinie (rF > 100 %)"),
    "İstenen SHF ile doğru %100 nem eğrisini kesmiyor. SHF'yi düşürün ya da SHF işlemini kullanın." to arrayOf(
        "With the requested SHF the line does not cross the 100% RH curve. Lower the SHF or use the SHF process.",
        "Con el SHF indicado la recta no corta la curva de 100 % HR. Reduzca el SHF o use el proceso SHF.",
        "Avec le SHF demandé, la droite ne coupe pas la courbe 100 % HR. Réduisez le SHF ou utilisez le processus SHF.",
        "Mit dem gewünschten SHF schneidet die Linie die 100-%-Kurve nicht. SHF verringern oder den SHF-Prozess verwenden."),
    "İstenen soğutma ve SHF %100 nem eğrisini aşıyor. Kapasiteyi azaltın, SHF'yi değiştirin ya da doymaya soğutma + yoğuşma kullanın." to arrayOf(
        "The requested cooling and SHF exceed the 100% RH curve. Reduce capacity, change the SHF or use cooling to saturation + condensation.",
        "El enfriamiento y el SHF indicados superan la curva de 100 % HR. Reduzca la capacidad, cambie el SHF o use enfriamiento hasta saturación + condensación.",
        "Le refroidissement et le SHF demandés dépassent la courbe 100 % HR. Réduisez la puissance, modifiez le SHF ou utilisez refroidissement jusqu'à saturation + condensation.",
        "Gewünschte Kühlung und SHF überschreiten die 100-%-Kurve. Leistung verringern, SHF ändern oder Kühlung bis zur Sättigung + Kondensation verwenden."),
    "Buharlı nemlendirme sonucu %100 nem eğrisini aşıyor. Miktarı azaltın." to arrayOf(
        "Steam humidification result exceeds the 100% RH curve. Reduce the amount.",
        "El resultado de la humidificación por vapor supera la curva de 100 % HR. Reduzca la cantidad.",
        "Le résultat de l'humidification vapeur dépasse la courbe 100 % HR. Réduisez la quantité.",
        "Ergebnis der Dampfbefeuchtung überschreitet die 100-%-Kurve. Menge verringern."),
    "Sulu nemlendirme sonucu diyagram dışında. Miktarı azaltın ya da buharlı nemlendirme kullanın." to arrayOf(
        "Water humidification result is outside the chart. Reduce the amount or use steam humidification.",
        "El resultado de la humidificación por agua está fuera del diagrama. Reduzca la cantidad o use humidificación por vapor.",
        "Le résultat de l'humidification par eau est hors du diagramme. Réduisez la quantité ou utilisez l'humidification vapeur.",
        "Ergebnis der Wasserbefeuchtung liegt außerhalb des Diagramms. Menge verringern oder Dampfbefeuchtung verwenden."),
    "Nem alma sonucu nem oranı negatif. Miktarı azaltın." to arrayOf(
        "Dehumidification gives a negative humidity ratio. Reduce the amount.",
        "La deshumidificación da una humedad absoluta negativa. Reduzca la cantidad.",
        "La déshumidification donne une humidité absolue négative. Réduisez la quantité.",
        "Entfeuchtung ergibt einen negativen Feuchtegehalt. Menge verringern."),
    "Yoğuşma işlemi için iki nokta da %100 nem eğrisinde olmalı." to arrayOf(
        "For condensation both points must be on the 100% RH curve.",
        "Para la condensación ambos puntos deben estar en la curva de 100 % HR.",
        "Pour la condensation, les deux points doivent être sur la courbe 100 % HR.",
        "Für Kondensation müssen beide Punkte auf der 100-%-Kurve liegen."),
    "SHF doğrusu %100 nem eğrisini kesmiyor." to arrayOf("The SHF line does not cross the 100% RH curve.", "La recta SHF no corta la curva de 100 % HR.",
        "La droite SHF ne coupe pas la courbe 100 % HR.", "Die SHF-Linie schneidet die 100-%-Kurve nicht."),
    "Nokta yok" to arrayOf("Point missing", "Falta el punto", "Point manquant", "Punkt fehlt"),
    "İşlem beklenen çıktıları üretmedi" to arrayOf("The process did not produce the expected outputs", "El proceso no produjo las salidas esperadas",
        "Le processus n'a pas produit les sorties attendues", "Der Prozess hat die erwarteten Ausgänge nicht erzeugt"),

    // PDF rapor
    "Psikrometri raporu" to arrayOf("Psychrometric report", "Informe psicrométrico", "Rapport psychrométrique", "Psychrometrischer Bericht"),
    "rakım" to arrayOf("altitude", "altitud", "altitude", "Höhe"),
    "basınç" to arrayOf("pressure", "presión", "pression", "Druck"),
    "Nokta" to arrayOf("Point", "Punto", "Point", "Punkt"),
    "debi" to arrayOf("flow", "caudal", "débit", "Strom"),
    "Hata" to arrayOf("Error", "Error", "Erreur", "Fehler"),
)

/** Yüzde yazımı dile göre: %75 (TR), 75% (EN), 75 % (ES/FR/DE). */
fun yuzde(s: String) = when (dil) { Dil.TR -> "%$s"; Dil.EN -> "$s%"; else -> "$s %" }
val ondalikAyrac get() = if (dil == Dil.EN) '.' else ','
