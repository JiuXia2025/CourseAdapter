package parser.wakeup

import bean.Course
import main.java.bean.TimeDetail
import main.java.bean.TimeTable
import org.jsoup.Jsoup
import parser.Parser

class GXICParser(source: String) : Parser(source) {

    private val nodeMap = mapOf(
        "第0102节" to 1,
        "第0304节" to 3,
        "第0506节" to 5,
        "第0708节" to 7,
        "第0910节" to 9
    )

    override fun generateCourseList(): List<Course> {
        val courseList = arrayListOf<Course>()
        val doc = Jsoup.parse(source)
        val kbtable = doc.select("table[border=1]").first() ?: return courseList
        val trs = kbtable.select("tr")

        for ((rowIndex, tr) in trs.withIndex()) {
            if (rowIndex == 0) continue // 跳过表头行

            val tds = tr.select("td")
            if (tds.isEmpty()) continue // 跳过空行

            val nodeText = tds[0].text().trim()
            val startNode = nodeMap[nodeText] ?: continue // 如果节次信息不在 map 中，跳过

            for ((colIndex, td) in tds.withIndex()) {
                if (colIndex == 0) continue // 跳过第一列（节次信息）

                val courseElements = td.select("a")
                if (courseElements.isEmpty()) continue // 跳过没有课程的单元格

                for (courseElement in courseElements) {
                    val courseHtml = courseElement.attr("title")
                    if (courseHtml.isBlank()) continue

                    val courseName = courseHtml.substringAfter("课程名称：").substringBefore("\n").trim()
                    val teacher = courseHtml.substringAfter("授课教师：").substringBefore("\n").trim()
                    val room = courseHtml.substringAfter("开课地点：").substringBefore("\n").trim()
                    val weekStr = courseHtml.substringAfter("上课周次：").substringBefore("\n").trim()

                    val weekList = weekStr.split(',')
                    var startWeek = 0
                    var endWeek = 0
                    var type = 0

                    weekList.forEach {
                        if (it.contains('-')) {
                            val weeks = it.split('-')
                            startWeek = weeks[0].toIntOrNull() ?: startWeek
                            endWeek = weeks[1].substringBefore('(').replace("单", "").replace("双", "").toIntOrNull() ?: endWeek
                            type = when {
                                weeks[1].contains('单') -> 1
                                weeks[1].contains('双') -> 2
                                else -> 0
                            }
                        } else {
                            startWeek = it.substringBefore('(').replace("单", "").replace("双", "").toIntOrNull() ?: startWeek
                            endWeek = startWeek
                        }
                        courseList.add(
                            Course(
                                name = courseName,
                                room = room,
                                teacher = teacher,
                                day = colIndex,
                                startNode = startNode,
                                endNode = startNode + 1,
                                startWeek = startWeek,
                                endWeek = endWeek,
                                type = type
                            )
                        )
                    }
                }
            }
        }
        return courseList
    }

    override fun generateTimeTable(): TimeTable {
        return buildTimeTable("广西工职院武鸣三校区") {
            add("08:40", "09:20")
            add("09:30", "10:10")
            add("10:30", "11:10")
            add("11:20", "12:00")
            add("14:30", "15:10")
            add("15:20", "16:00")
            add("16:10", "16:50")
            add("17:00", "17:40")
            add("19:40", "20:20")
            add("20:30", "21:10")
        }
    }

    private fun buildTimeTable(name: String, block: TimeTableBuilder.() -> Unit): TimeTable {
        val builder = TimeTableBuilder(name)
        builder.block()
        return builder.build()
    }

    private class TimeTableBuilder(private val name: String) {
        private val timeList = mutableListOf<TimeDetail>()
        private var nodeCounter = 1

        fun add(startTime: String, endTime: String) {
            timeList.add(TimeDetail(nodeCounter++, startTime, endTime))
        }

        fun build(): TimeTable {
            return TimeTable(name, timeList)
        }
    }
}
