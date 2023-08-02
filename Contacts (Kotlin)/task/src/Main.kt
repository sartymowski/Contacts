package contacts

import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.ZonedDateTime
import com.squareup.moshi.*
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import java.io.File
import java.lang.Exception

abstract class Record(phoneNumber: String) {

    private var availableProperty = setOf("number")

    protected fun updateLastEditingDateTime() {
        this.lastEditingDateTime = ZonedDateTime.now().toLocalDateTime().withNano(0).toString()
    }

    protected fun String.assignDefaultDataIfEmpty() = this.let {
        if (it.trim().isEmpty()) {
            "[no data]"
        } else {
            it
        }
    }

    open fun getAllAvailableProperties() = availableProperty


    open fun changeProperty(property: String, newValue: String): Boolean {
        if (availableProperty.none { it == property }) {
            return false
        }

        when (property) {
            "number" -> {
                phoneNumber = newValue
            }

            else -> {
                assert(false)
                return false
            }
        }

        return true
    }

    @Json(name = "phone-number")
    var phoneNumber: String = phoneNumber.assignDefaultDataIfEmpty()
        set(value) {
            /*val regex =
                """(\+)?(\w+([ -]\(\w{2,}\))?|\(\w+\)([ -]?\w{2,})?|\w+[ -]?\w{2,})([ -](\w{2,}))?([ -](\w{2,}))?""".toRegex()*/
            val regex = """(\+)?(\(?\w+\)?)([ -](\(\w{2,}\)))?([ -](\w{2,}))?([ -](\w{2,}))?([ -](\w{2,}))?""".toRegex()
            if (value.matches(regex)) {
                field = value.assignDefaultDataIfEmpty()
            } else {
                println("Wrong number format!")
                field = "[no number]"
            }
            updateLastEditingDateTime()
        }

    @Transient
    private var creationDateTime: String = ZonedDateTime.now().toLocalDateTime().withNano(0).toString()

    @Transient
    private var lastEditingDateTime: String = creationDateTime

    abstract fun fullInfo(): String
    override fun toString(): String =
        StringBuilder().appendLine("Number: $phoneNumber").appendLine("Time created: $creationDateTime")
            .appendLine("Time last edit: $lastEditingDateTime")
            .toString()
}

@JsonClass(generateAdapter = true)
class RecordCompany(organizationName: String, address: String, phoneNumber: String) : Record(phoneNumber) {

    @Transient
    private var availableProperty = setOf("name", "address")

    override fun getAllAvailableProperties(): Set<String> {
        val properties = availableProperty.toMutableSet()
        properties.addAll(super.getAllAvailableProperties())
        return properties.toSet()
    }

    override fun changeProperty(property: String, newValue: String): Boolean {
        if (availableProperty.none { it == property }) {
            return super.changeProperty(property, newValue)
        }

        when (property) {
            "name" -> {
                organizationName = newValue
            }

            "address" -> {
                address = newValue
            }

            else -> {
                assert(false)
                return false
            }
        }

        return true
    }

    @Json(name = "organization name")
    var organizationName: String = organizationName.assignDefaultDataIfEmpty()
        set(value) {
            field = value.assignDefaultDataIfEmpty()
            updateLastEditingDateTime()
        }


    @Json(name = "address")
    var address: String = address.assignDefaultDataIfEmpty()
        set(value) {
            field = value.assignDefaultDataIfEmpty()
            updateLastEditingDateTime()
        }

    override fun fullInfo() =
        StringBuilder().appendLine("Organization name: $organizationName").appendLine("Address: $address")
            .appendLine(super.toString()).toString()

    override fun toString() = organizationName

}

@JsonClass(generateAdapter = true)
class RecordPerson(
    name: String,
    surname: String,
    birthDate: String,
    gender: String,
    phoneNumber: String
) : Record(phoneNumber) {

    @Transient
    private var availableProperty = setOf("name", "surname", "birth", "gender")

    override fun getAllAvailableProperties(): Set<String> {
        val properties = availableProperty.toMutableSet()
        properties.addAll(super.getAllAvailableProperties())
        return properties.toSet()
    }

    override fun changeProperty(property: String, newValue: String): Boolean {
        if (availableProperty.none { it == property }) {
            return super.changeProperty(property, newValue)
        }

        when (property) {
            "name" -> {
                name = newValue
            }

            "surname" -> {
                surname = newValue
            }

            "birth" -> {
                birthDate = newValue
            }

            "gender" -> {
                gender = newValue
            }

            else -> {
                assert(false)
                return false
            }
        }

        return true
    }

    var name: String = name.assignDefaultDataIfEmpty()
        set(value) {
            field = value
            updateLastEditingDateTime()
        }

    var surname: String = surname.assignDefaultDataIfEmpty()
        set(value) {
            field = value
            updateLastEditingDateTime()
        }

    var birthDate: String = birthDate.assignDefaultDataIfEmpty()
        set(value) {
            try {
                field = LocalDate.parse(value).toString()
            } catch (ex: DateTimeParseException) {
                println("Bad birth date!")
                field = "[no data]"
            }

            updateLastEditingDateTime()
        }

    companion object {
        val allowedGenders = mutableListOf("M", "F")
    }

    var gender: String = gender.assignDefaultDataIfEmpty()
        set(value) {
            if (allowedGenders.none { it == value }) {
                field = "[no data]"
                println("Bad gender!")
            } else {
                field = value
            }

            updateLastEditingDateTime()
        }

    override fun fullInfo() =
        StringBuilder().appendLine("Name: $name").appendLine("Surname: $surname").appendLine("Birth date: $birthDate")
            .appendLine("Gender: $gender").appendLine(super.toString()).toString()

    override fun toString() = "$name $surname"

}

class Contacts(private val filename: String? = null) {
    private val records = mutableListOf<Record>()

    init {
        readFile()
    }

    private fun count() = run { println("The Phone Book has ${records.size} records.") }

    private fun saveFile() {
        if (filename == null) {
            return
        }

        val moshi = Moshi.Builder()
            .add(
                PolymorphicJsonAdapterFactory.of(Record::class.java, "phoneNumber")
                    .withSubtype(RecordCompany::class.java, "company")
                    .withSubtype(RecordPerson::class.java, "person")
            ).build()

        val type = Types.newParameterizedType(List::class.java, Record::class.java)
        val listAdapter = moshi.adapter<List<Record>>(type)

        val jsonFile = File(filename)
        jsonFile.writeText(listAdapter.toJson(records))
    }

    private fun readFile() {
        if (filename == null) {
            return
        }

        val jsonFile = File(filename)
        if (!jsonFile.exists()) {
            return
        }

        val moshi = Moshi.Builder()
            .add(
                PolymorphicJsonAdapterFactory.of(Record::class.java, "phoneNumber")
                    .withSubtype(RecordCompany::class.java, "company")
                    .withSubtype(RecordPerson::class.java, "person")
            ).build()

        val type = Types.newParameterizedType(List::class.java, Record::class.java)
        val listAdapter = moshi.adapter<List<Record?>>(type)
        if (jsonFile.length().toInt() == 0)
            return
        val tempRecords = listAdapter.fromJson(jsonFile.readText())
        tempRecords?.let { records.addAll(it.filterNotNull()) }
    }

    fun createPhoneBook() {
        while (true) {
            print("Enter action (add, list, search, count, exit): ")

            when (readln()) {
                "add" -> addRecord()
                "search" -> search()
                "count" -> count()
                "list" -> listRecords(internal = false)
                "exit" -> break
            }
        }
    }

    private fun addRecordPerson() {
        print("Enter the name: ")
        val name = readln()
        print("Enter the surname: ")
        val surname = readln()
        print("Enter the birth date: ")
        val birthDate = readln()
        print("Enter the gender (M, F): ")
        val gender = readln()
        print("Enter the number: ")
        val number = readln()

        records.add(RecordPerson(name, surname, birthDate, gender, number))
    }

    private fun addRecordCompany() {
        print("Enter the organization name: ")
        val organizationName = readln()
        print("Enter the address: ")
        val address = readln()
        print("Enter the number: ")
        val number = readln()

        records.add(RecordCompany(organizationName, address, number))
    }

    private fun addRecord() {
        print("Enter the type (person, organization): ")
        when (readln()) {
            "person" -> addRecordPerson()
            "organization" -> addRecordCompany()
            else -> throw Exception("you must chose an operation")
        }
        saveFile()

        println("The record added.\n")
    }

    private fun search() {
        while(true) {
            print("Enter search query: ")
            val query = readln()
            val foundRecords =
                records.mapIndexed { index, it -> Pair(index, it) }.filter { it.second.fullInfo().contains(query, true) }
            if (foundRecords.isNotEmpty()) {
                for (record in foundRecords.withIndex()) {
                    println("${record.index + 1}. ${record.value.second}")
                }

                print("[search] Enter action ([number], back, again): ")
                when (val action = readln()) {
                    "back" -> return
                    "again" -> continue
                    else -> {
                        val index = action.toInt() - 1
                        print(records[index].fullInfo())
                        recordOperations(index)
                        return
                    }
                }
            }
        }
    }

    private fun editRecord(index: Int) {
        val properties = records[index].getAllAvailableProperties()
        print("Select a field ${properties.joinToString(prefix = "(", postfix = ")")}: ")
        val field = readln()
        if (!properties.contains(field)) {
            return
        }
        print("Enter $field: ")
        records[index].changeProperty(field, readln())
        saveFile()

        println("Saved\n")
        print(records[index].fullInfo())
    }

    private fun recordOperations(index: Int) {
        print("[record] Enter action (edit, delete, menu): ")
        while(true) {
            when (readln()) {
                "edit" -> {
                    editRecord(index)
                }

                "delete" -> {
                    records.removeAt(index)
                    println("The record removed!")
                }

                "menu" -> {
                    return
                }
            }
        }
    }

    private fun listRecords(internal: Boolean) {
        for (record in records.withIndex()) {
            println("${record.index + 1}. ${record.value}")
        }
        if (!internal && records.isNotEmpty()) {
            print("[list] Enter action ([number], back): ")
            when (val action = readln()) {
                "back" -> {
                    return
                }

                else -> {
                    val index = action.toInt() - 1
                    print(records[index].fullInfo())
                    recordOperations(index)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    Contacts(args.firstOrNull()).createPhoneBook()
}