package com.fengshui.app.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fengshui.app.data.Project
import com.fengshui.app.data.PointRepository
import com.fengshui.app.data.FengShuiPoint
import com.fengshui.app.data.PointType
import com.fengshui.app.map.ui.RenamePointDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CaseListScreen - 堪舆案例管理界面
 *
 * 展示所有案例，支持：
 * - 创建新案例
 * - 编辑案例名称和描述
 * - 删除案例（级联删除点位）
 * - 展开/收起查看案例内的点位
 * - Phase 3.1: 快速加点功能
 */
@Composable
fun CaseListScreen(
    modifier: Modifier = Modifier,
    onQuickAddPoint: (caseId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val repo = remember { PointRepository(context) }
    val scope = rememberCoroutineScope()

    var projects by remember { mutableStateOf(listOf<Project>()) }
    var expandedCaseId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<Project?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    // 初始化加载案例列表
    val loadProjects = {
        projects = repo.loadProjects()
    }

    // 初次加载
    remember { loadProjects() }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (projects.isEmpty()) {
                // 空状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("暂无案例", fontSize = 16.sp, color = Color.Gray)
                    Text("点击右下角按钮创建新案例", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                // 案例列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 80.dp)
                ) {
                    items(projects) { project ->
                        CaseListItem(
                            project = project,
                            isExpanded = expandedCaseId == project.id,
                            onToggleExpand = { expandedCaseId = if (expandedCaseId == project.id) null else project.id },
                            onEdit = {
                                projectToEdit = project
                                showEditDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    repo.deleteProject(project.id)
                                    loadProjects()
                                    if (expandedCaseId == project.id) {
                                        expandedCaseId = null
                                    }
                                }
                            },
                            onQuickAddPoint = { caseId ->
                                onQuickAddPoint(caseId)
                            },
                            repo = repo
                        )
                    }
                }
            }

            // 创建新案例按钮
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建案例")
            }

            // 创建案例对话框
            if (showCreateDialog) {
                CreateCaseDialog(
                    onConfirm = { name, description ->
                        scope.launch {
                            repo.createProject(name, description)
                            loadProjects()
                            showCreateDialog = false
                        }
                    },
                    onDismiss = { showCreateDialog = false }
                )
            }

            // 编辑案例对话框
            if (showEditDialog && projectToEdit != null) {
                EditCaseDialog(
                    project = projectToEdit!!,
                    onConfirm = { name, description ->
                        scope.launch {
                            val updated = projectToEdit!!.copy(
                                name = name,
                                description = description,
                                updateTime = System.currentTimeMillis()
                            )
                            repo.updateProject(updated)
                            loadProjects()
                            showEditDialog = false
                            projectToEdit = null
                        }
                    },
                    onDismiss = {
                        showEditDialog = false
                        projectToEdit = null
                    }
                )
            }
        }
    }
}

@Composable
private fun CaseListItem(
    project: Project,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickAddPoint: (caseId: String) -> Unit = {},
    repo: PointRepository
) {
    val context = LocalContext.current
    var points by remember { mutableStateOf(repo.getPointsByCase(project.id)) }
    var pointToRename by remember { mutableStateOf<FengShuiPoint?>(null) }
    val scope = rememberCoroutineScope()

    val originCount = points.count { it.type == PointType.ORIGIN }
    val destCount = points.count { it.type == PointType.DESTINATION }

    val reloadPoints = {
        points = repo.getPointsByCase(project.id)
    }

    LaunchedEffect(project.id) {
        reloadPoints()
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val createTime = remember { dateFormat.format(Date(project.createTime)) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        project.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "原点: $originCount | 终点: $destCount | 创建: $createTime",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "展开/收起"
                )
            }

            // 展开内容
            if (isExpanded) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // 案例描述
                    if (!project.description.isNullOrEmpty()) {
                        Text(
                            "描述: ${project.description}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // 点位列表
                    if (points.isEmpty()) {
                        Text(
                            "该案例中暂无点位",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            points.forEach { point ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color(0xFFF5F5F5),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "[${if (point.type == PointType.ORIGIN) "原" else "终"}] ${point.name}",
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "纬: %.4f, 经: %.4f".format(point.latitude, point.longitude),
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    IconButton(onClick = { pointToRename = point }) {
                                        Icon(Icons.Default.Edit, contentDescription = "重命名")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            repo.deletePoint(point.id)
                                            reloadPoints()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除点位")
                                    }
                                }
                            }
                        }
                    }

                    // 编辑、快速加点和删除按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = onEdit, 
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("编辑")
                        }
                        Button(
                            onClick = { onQuickAddPoint(project.id) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp, end = 8.dp)
                        ) {
                            Text("快速加点")
                        }
                        Button(
                            onClick = onDelete,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text("删除")
                        }
                    }
                }
            }
        }
    }

    if (pointToRename != null) {
        RenamePointDialog(
            pointName = pointToRename!!.name,
            onConfirm = { newName ->
                scope.launch {
                    repo.updatePoint(pointToRename!!.copy(name = newName))
                    reloadPoints()
                    pointToRename = null
                }
            },
            onDismiss = { pointToRename = null }
        )
    }
}

@Composable
private fun CreateCaseDialog(
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建新案例") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("案例名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("案例描述（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim().ifEmpty { null } ?: description)
                    }
                }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EditCaseDialog(
    project: Project,
    onConfirm: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var description by remember { mutableStateOf(project.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑案例") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("案例名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("案例描述") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim())
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
