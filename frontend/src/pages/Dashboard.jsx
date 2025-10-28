import React, { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import axios from 'axios'
import { 
  Cloud, 
  Upload, 
  Download, 
  Trash2, 
  LogOut, 
  User, 
  File as FileIcon,
  Image,
  FileText,
  Film,
  Music,
  Folder,
  FolderPlus,
  ChevronRight,
  X,
  Grid,
  List,
  HardDrive
} from 'lucide-react'

const Dashboard = () => {
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [message, setMessage] = useState(null)
  const [currentFolderId, setCurrentFolderId] = useState(null)
  const [folderPath, setFolderPath] = useState([])
  const [showCreateFolder, setShowCreateFolder] = useState(false)
  const [newFolderName, setNewFolderName] = useState('')
  const [previewFile, setPreviewFile] = useState(null)
  const [previewContent, setPreviewContent] = useState('')
  const [viewMode, setViewMode] = useState('list')
  const [contextMenu, setContextMenu] = useState(null) // 右键菜单 { x, y, file }
  const [clipboard, setClipboard] = useState(null) // 剪贴板 { file, action: 'copy' | 'cut' }
  const [showRename, setShowRename] = useState(false) // 显示重命名对话框
  const [renameFile, setRenameFile] = useState(null) // 要重命名的文件
  const [newFileName, setNewFileName] = useState('') // 新文件名
  const fileInputRef = useRef(null)
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    fetchFiles()
  }, [])

  const fetchFiles = async (folderId = null) => {
    try {
      const url = folderId ? `/api/files?parentId=${folderId}` : '/api/files'
      const response = await axios.get(url)
      setFiles(response.data.data)
      setLoading(false)
    } catch (error) {
      console.error('获取文件列表失败:', error)
      setLoading(false)
    }
  }

  const handleFileUpload = async (e) => {
    const files = Array.from(e.target.files)
    if (files.length === 0) return

    setUploading(true)
    let successCount = 0
    let failCount = 0

    try {
      for (const file of files) {
        const formData = new FormData()
        formData.append('file', file)
        if (currentFolderId) {
          formData.append('parentId', currentFolderId)
        }

        try {
          await axios.post('/api/files/upload', formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          })
          successCount++
        } catch (error) {
          failCount++
          console.error('文件上传失败:', file.webkitRelativePath || file.name, error)
        }
      }
      
      if (failCount === 0) {
        setMessage({ type: 'success', text: `成功上传 ${successCount} 个文件！` })
      } else {
        setMessage({ type: 'error', text: `上传完成：成功 ${successCount} 个，失败 ${failCount} 个` })
      }
      
      fetchFiles(currentFolderId)
      e.target.value = ''
    } finally {
      setUploading(false)
    }
  }

  const handleDownload = async (fileId, fileName) => {
    try {
      const response = await axios.get(`/api/files/download/${fileId}`, {
        responseType: 'blob'
      })
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', fileName)
      document.body.appendChild(link)
      link.click()
      link.remove()
      setMessage({ type: 'success', text: '文件下载成功！' })
    } catch (error) {
      setMessage({ type: 'error', text: '文件下载失败' })
    }
  }

  const handleDelete = async (fileId, isFolder) => {
    const confirmText = isFolder ? '确定要删除这个文件夹吗？' : '确定要删除这个文件吗？'
    if (!window.confirm(confirmText)) return

    try {
      await axios.delete(`/api/files/${fileId}`)
      setMessage({ type: 'success', text: '删除成功！' })
      fetchFiles(currentFolderId)
    } catch (error) {
      setMessage({ type: 'error', text: '删除失败' })
    }
  }
  
  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) {
      setMessage({ type: 'error', text: '请输入文件夹名称' })
      return
    }

    try {
      const params = new URLSearchParams()
      params.append('folderName', newFolderName)
      if (currentFolderId) {
        params.append('parentId', currentFolderId)
      }
      
      await axios.post('/api/files/folder', params)
      setMessage({ type: 'success', text: '文件夹创建成功！' })
      setShowCreateFolder(false)
      setNewFolderName('')
      fetchFiles(currentFolderId)
    } catch (error) {
      setMessage({ type: 'error', text: '文件夹创建失败：' + (error.response?.data?.message || error.message) })
    }
  }
  
  const handleOpenFolder = (folder) => {
    setCurrentFolderId(folder.id)
    setFolderPath([...folderPath, { id: folder.id, name: folder.fileName }])
    fetchFiles(folder.id)
  }
  
  const handleNavigateToFolder = (index) => {
    if (index === -1) {
      // 返回根目录
      setCurrentFolderId(null)
      setFolderPath([])
      fetchFiles(null)
    } else {
      const folder = folderPath[index]
      setCurrentFolderId(folder.id)
      setFolderPath(folderPath.slice(0, index + 1))
      fetchFiles(folder.id)
    }
  }
  
  const handlePreviewFile = async (file) => {
    // 检查是否为文本文件
    const textTypes = ['text/', 'application/json', 'application/xml', 'application/javascript']
    const isTextFile = textTypes.some(type => file.fileType?.startsWith(type)) || 
                       file.fileName?.match(/\.(txt|md|json|xml|html|css|js|jsx|ts|tsx|java|py|cpp|c|h|sh|yml|yaml|properties|sql)$/i)
    
    if (!isTextFile) {
      setMessage({ type: 'error', text: '只能预览文本文件' })
      return
    }
    
    try {
      const response = await axios.get(`/api/files/preview/${file.id}`)
      setPreviewFile(file)
      setPreviewContent(response.data.data)
    } catch (error) {
      setMessage({ type: 'error', text: '预览失败：' + (error.response?.data?.message || error.message) })
    }
  }
  
  const handleDoubleClick = (file) => {
    if (file.isFolder) {
      handleOpenFolder(file)
    } else {
      handlePreviewFile(file)
    }
  }
  
  const closePreview = () => {
    setPreviewFile(null)
    setPreviewContent('')
  }

  // 右键菜单功能
  const handleContextMenu = (e, file) => {
    e.preventDefault()
    e.stopPropagation() // 阻止事件冒泡
    setContextMenu({ x: e.clientX, y: e.clientY, file })
  }
  
  const closeContextMenu = () => {
    setContextMenu(null)
  }
  
  const handleCopy = (file) => {
    setClipboard({ file, action: 'copy' })
    setMessage({ type: 'success', text: `已复制 "${file.fileName}"` })
    closeContextMenu()
  }
  
  const handleCut = (file) => {
    setClipboard({ file, action: 'cut' })
    setMessage({ type: 'success', text: `已剪切 "${file.fileName}"` })
    closeContextMenu()
  }
  
  const handlePaste = async () => {
    if (!clipboard) {
      setMessage({ type: 'error', text: '剪贴板为空' })
      return
    }
    
    try {
      if (clipboard.action === 'cut') {
        await axios.put(`/api/files/move/${clipboard.file.id}`, null, {
          params: { targetFolderId: currentFolderId }
        })
        setMessage({ type: 'success', text: `已移动 "${clipboard.file.fileName}"` })
        setClipboard(null)
      } else {
        setMessage({ type: 'error', text: '复制功能暂未实现' })
      }
      fetchFiles(currentFolderId)
    } catch (error) {
      setMessage({ type: 'error', text: '操作失败：' + (error.response?.data?.message || error.message) })
    }
    closeContextMenu()
  }
  
  const handleRenameClick = (file) => {
    setRenameFile(file)
    setNewFileName(file.fileName)
    setShowRename(true)
    closeContextMenu()
  }
  
  const handleRenameSubmit = async () => {
    if (!newFileName.trim()) {
      setMessage({ type: 'error', text: '文件名不能为空' })
      return
    }
    
    try {
      await axios.put(`/api/files/rename/${renameFile.id}`, null, {
        params: { newName: newFileName }
      })
      setMessage({ type: 'success', text: '重命名成功' })
      setShowRename(false)
      setRenameFile(null)
      setNewFileName('')
      fetchFiles(currentFolderId)
    } catch (error) {
      setMessage({ type: 'error', text: '重命名失败：' + (error.response?.data?.message || error.message) })
    }
  }

  // 删除以下的拖拽上传功能
  const handleDragEnter = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(true)
  }
  
  const handleDragLeave = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }
  
  const handleDragOver = (e) => {
    e.preventDefault()
    e.stopPropagation()
  }
  
  const handleDrop = async (e) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
    
    const items = e.dataTransfer.items
    const files = []
    
    for (let i = 0; i < items.length; i++) {
      const item = items[i].webkitGetAsEntry()
      if (item) {
        await traverseFileTree(item, files)
      }
    }
    
    if (files.length > 0) {
      uploadFiles(files)
    }
  }
  
  const traverseFileTree = async (item, files, path = '') => {
    return new Promise((resolve) => {
      if (item.isFile) {
        item.file((file) => {
          Object.defineProperty(file, 'webkitRelativePath', {
            value: path + file.name,
            writable: false
          })
          files.push(file)
          resolve()
        })
      } else if (item.isDirectory) {
        const dirReader = item.createReader()
        dirReader.readEntries(async (entries) => {
          for (let i = 0; i < entries.length; i++) {
            await traverseFileTree(entries[i], files, path + item.name + '/')
          }
          resolve()
        })
      }
    })
  }
  
  const uploadFiles = async (fileList) => {
    setUploading(true)
    let successCount = 0
    let failCount = 0

    try {
      for (const file of fileList) {
        const formData = new FormData()
        formData.append('file', file)
        if (currentFolderId) {
          formData.append('parentId', currentFolderId)
        }

        try {
          await axios.post('/api/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          successCount++
        } catch (error) {
          failCount++
          console.error('文件上传失败:', file.webkitRelativePath || file.name, error)
        }
      }
      
      if (failCount === 0) {
        setMessage({ type: 'success', text: `成功上传 ${successCount} 个文件！` })
      } else {
        setMessage({ type: 'error', text: `上传完成：成功 ${successCount} 个，失败 ${failCount} 个` })
      }
      
      fetchFiles(currentFolderId)
    } finally {
      setUploading(false)
    }
  }

  // 文件拖拽移动功能
  const handleFileDragStart = (e, file) => {
    e.stopPropagation()
    console.log('开始拖拽:', file.fileName)
    setDraggedFile(file)
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', file.id) // 设置拖拽数据
  }
  
  const handleFileDragOver = (e, file) => {
    e.preventDefault()
    e.stopPropagation()
    
    if (!draggedFile) return
    
    console.log('拖拽经过:', file.fileName, '是文件夹:', file.isFolder, '当前拖拽的文件:', draggedFile?.fileName)
    
    if (file.isFolder && draggedFile.id !== file.id) {
      e.dataTransfer.dropEffect = 'move'
      setDropTarget(file.id)
      console.log('设置放置目标:', file.fileName)
    } else {
      e.dataTransfer.dropEffect = 'none'
      setDropTarget(null)
    }
  }
  
  const handleFileDragLeave = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDropTarget(null)
  }
  
  const handleFileDrop = async (e, targetFolder) => {
    e.preventDefault()
    e.stopPropagation()
    setDropTarget(null)
    
    if (!draggedFile) return
    if (!targetFolder.isFolder) return
    
    try {
      await axios.put(`/api/files/move/${draggedFile.id}`, null, {
        params: { targetFolderId: targetFolder.id }
      })
      setMessage({ type: 'success', text: '移动成功！' })
      fetchFiles(currentFolderId)
    } catch (error) {
      setMessage({ type: 'error', text: '移动失败：' + (error.response?.data?.message || error.message) })
    } finally {
      setDraggedFile(null)
    }
  }
  
  const handleFileDragEnd = () => {
    setDraggedFile(null)
    setDropTarget(null)
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  const getFileIcon = (file) => {
    if (file.isFolder) return <Folder className="w-6 h-6 text-yellow-500" />
    if (file.fileType?.startsWith('image/')) return <Image className="w-6 h-6 text-blue-500" />
    if (file.fileType?.startsWith('video/')) return <Film className="w-6 h-6 text-purple-500" />
    if (file.fileType?.startsWith('audio/')) return <Music className="w-6 h-6 text-green-500" />
    if (file.fileType?.includes('text') || file.fileType?.includes('document')) return <FileText className="w-6 h-6 text-orange-500" />
    return <FileIcon className="w-6 h-6 text-gray-500" />
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="bg-blue-500 p-2 rounded-lg">
                <Cloud className="w-6 h-6 text-white" />
              </div>
              <h1 className="text-2xl font-bold text-gray-800">云存储系统</h1>
            </div>
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2 text-gray-700">
                <User className="w-5 h-5" />
                <span className="font-medium">{user?.username}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center space-x-2 px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition"
              >
                <LogOut className="w-4 h-4" />
                <span>退出</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
      {/* Message */}
      {message && (
        <div className={`mx-6 mt-4 p-3 rounded-lg flex items-center justify-between ${
          message.type === 'success' 
            ? 'bg-green-50 border border-green-200 text-green-700' 
            : 'bg-red-50 border border-red-200 text-red-700'
        }`}>
          <span className="text-sm">{message.text}</span>
          <button onClick={() => setMessage(null)} className="text-lg font-bold hover:opacity-70">×</button>
        </div>
      )}

      {/* 工具栏 */}
      <div className="bg-white border-b border-gray-200 px-6 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Upload className="w-4 h-4" />
              <span className="text-sm">{uploading ? '上传中...' : '上传'}</span>
            </button>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              onChange={handleFileUpload}
              disabled={uploading}
              className="hidden"
            />
            <button
              onClick={() => setShowCreateFolder(true)}
              className="flex items-center space-x-2 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition"
            >
              <FolderPlus className="w-4 h-4" />
              <span className="text-sm">新建文件夹</span>
            </button>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-lg transition ${
                viewMode === 'list' ? 'bg-blue-50 text-blue-600' : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <List className="w-5 h-5" />
            </button>
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded-lg transition ${
                viewMode === 'grid' ? 'bg-blue-50 text-blue-600' : 'text-gray-600 hover:bg-gray-100'
              }`}
            >
              <Grid className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* 面包屑导航 */}
      <div className="bg-white border-b border-gray-200 px-6 py-2">
        <div className="flex items-center space-x-1 text-sm">
          <button
            onClick={() => handleNavigateToFolder(-1)}
            className="flex items-center space-x-1 px-2 py-1 text-gray-700 hover:bg-gray-100 rounded transition"
          >
            <HardDrive className="w-4 h-4" />
            <span>我的云盘</span>
          </button>
          {folderPath.map((folder, index) => (
            <React.Fragment key={folder.id}>
              <ChevronRight className="w-4 h-4 text-gray-400" />
              <button
                onClick={() => handleNavigateToFolder(index)}
                className="px-2 py-1 text-gray-700 hover:bg-gray-100 rounded transition"
              >
                {folder.name}
              </button>
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* 文件列表区域 */}
      <div 
        className="flex-1 overflow-auto bg-white"
        onContextMenu={(e) => {
          e.preventDefault()
          // 在空白区域右键，显示简化菜单（只有粘贴选项）
          setContextMenu({ x: e.clientX, y: e.clientY, file: null })
        }}
        onClick={closeContextMenu}
      >
        
        {loading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-4 border-blue-500 border-t-transparent"></div>
            <p className="mt-4 text-gray-600">加载中...</p>
          </div>
        ) : files.length === 0 ? (
          <div className="text-center py-12">
            <FileIcon className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500">还没有上传任何文件</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">文件</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">大小</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">上传时间</th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {files.map((file) => (
                  <tr 
                    key={file.id} 
                    className="hover:bg-gray-50 transition cursor-pointer"
                    onContextMenu={(e) => handleContextMenu(e, file)}
                    onDoubleClick={() => handleDoubleClick(file)}
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center space-x-3">
                        {getFileIcon(file)}
                        <span className="text-sm font-medium text-gray-900">{file.fileName}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {file.isFolder ? '-' : formatFileSize(file.fileSize)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                      {new Date(file.uploadedAt).toLocaleString('zh-CN')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      {!file.isFolder && (
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              handleDownload(file.id, file.fileName)
                            }}
                            className="inline-flex items-center space-x-1 text-blue-600 hover:text-blue-800 mr-4"
                          >
                            <Download className="w-4 h-4" />
                            <span>下载</span>
                          </button>
                        )}
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            handleDelete(file.id, file.isFolder)
                          }}
                          className="inline-flex items-center space-x-1 text-red-600 hover:text-red-800"
                        >
                          <Trash2 className="w-4 h-4" />
                          <span>删除</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>
      
      {/* 右键菜单 */}
      {contextMenu && (
        <div 
          className="fixed bg-white rounded-lg shadow-xl border border-gray-200 py-2 min-w-[180px] z-50"
          style={{ top: contextMenu.y, left: contextMenu.x }}
          onClick={(e) => e.stopPropagation()}
        >
          {contextMenu.file ? (
            // 文件/文件夹菜单
            <>
              <button
                onClick={() => handleCopy(contextMenu.file)}
                className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center space-x-2"
              >
                <FileIcon className="w-4 h-4" />
                <span>复制</span>
              </button>
              <button
                onClick={() => handleCut(contextMenu.file)}
                className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center space-x-2"
              >
                <FileIcon className="w-4 h-4" />
                <span>剪切</span>
              </button>
              {clipboard && (
                <button
                  onClick={handlePaste}
                  className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center space-x-2"
                >
                  <FileIcon className="w-4 h-4" />
                  <span>粘贴</span>
                </button>
              )}
              <div className="border-t border-gray-200 my-1"></div>
              <button
                onClick={() => handleRenameClick(contextMenu.file)}
                className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center space-x-2"
              >
                <FileText className="w-4 h-4" />
                <span>重命名</span>
              </button>
              <button
                onClick={() => {
                  handleDelete(contextMenu.file.id, contextMenu.file.isFolder)
                  closeContextMenu()
                }}
                className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center space-x-2 text-red-600"
              >
                <Trash2 className="w-4 h-4" />
                <span>删除</span>
              </button>
              {!contextMenu.file.isFolder && (
                <>
                  <div className="border-t border-gray-200 my-1"></div>
                  <button
                    onClick={() => {
                      handleDownload(contextMenu.file.id, contextMenu.file.fileName)
                      closeContextMenu()
                    }}
                    className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center space-x-2"
                  >
                    <Download className="w-4 h-4" />
                    <span>下载</span>
                  </button>
                </>
              )}
            </>
          ) : (
            // 空白区域菜单（只有粘贴）
            <>
              {clipboard ? (
                <button
                  onClick={handlePaste}
                  className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center space-x-2"
                >
                  <FileIcon className="w-4 h-4" />
                  <span>粘贴</span>
                </button>
              ) : (
                <div className="px-4 py-2 text-sm text-gray-400">
                  没有可粘贴的内容
                </div>
              )}
            </>
          )}
        </div>
      )}
      
      {/* 重命名对话框 */}
      {showRename && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-96">
            <h3 className="text-lg font-semibold mb-4">重命名</h3>
            <input
              type="text"
              value={newFileName}
              onChange={(e) => setNewFileName(e.target.value)}
              placeholder="请输入新文件名"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4"
              onKeyPress={(e) => e.key === 'Enter' && handleRenameSubmit()}
              autoFocus
            />
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => {
                  setShowRename(false)
                  setRenameFile(null)
                  setNewFileName('')
                }}
                className="px-4 py-2 text-gray-600 hover:text-gray-800"
              >
                取消
              </button>
              <button
                onClick={handleRenameSubmit}
                className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
              >
                确定
              </button>
            </div>
          </div>
        </div>
      )}
      
      {/* 创建文件夹对话框 */}
      {showCreateFolder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-96">
            <h3 className="text-lg font-semibold mb-4">创建新文件夹</h3>
            <input
              type="text"
              value={newFolderName}
              onChange={(e) => setNewFolderName(e.target.value)}
              placeholder="请输入文件夹名称"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 mb-4"
              onKeyPress={(e) => e.key === 'Enter' && handleCreateFolder()}
            />
            <div className="flex justify-end space-x-3">
              <button
                onClick={() => {
                  setShowCreateFolder(false)
                  setNewFolderName('')
                }}
                className="px-4 py-2 text-gray-600 hover:text-gray-800"
              >
                取消
              </button>
              <button
                onClick={handleCreateFolder}
                className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
              >
                创建
              </button>
            </div>
          </div>
        </div>
      )}
      
      {/* 文本文件预览对话框 */}
      {previewFile && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between p-4 border-b">
              <h3 className="text-lg font-semibold">{previewFile.fileName}</h3>
              <button
                onClick={closePreview}
                className="text-gray-500 hover:text-gray-700"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
            <div className="flex-1 overflow-auto p-4">
              <pre className="text-sm bg-gray-50 p-4 rounded-lg overflow-x-auto">
                <code>{previewContent}</code>
              </pre>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Dashboard
