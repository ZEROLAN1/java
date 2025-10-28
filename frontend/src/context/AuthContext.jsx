import React, { createContext, useState, useContext, useEffect } from 'react'
import axios from 'axios'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('token')
    const username = localStorage.getItem('username')
    const email = localStorage.getItem('email')
    
    if (token && username && email) {
      setUser({ username, email })
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    }
    setLoading(false)
  }, [])

  const login = async (username, password) => {
    const response = await axios.post('/api/auth/login', { username, password })
    const { token, username: user, email } = response.data.data
    
    localStorage.setItem('token', token)
    localStorage.setItem('username', user)
    localStorage.setItem('email', email)
    
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    setUser({ username: user, email })
  }

  const register = async (username, email, password) => {
    const response = await axios.post('/api/auth/register', { username, email, password })
    const { token, username: user, email: userEmail } = response.data.data
    
    localStorage.setItem('token', token)
    localStorage.setItem('username', user)
    localStorage.setItem('email', userEmail)
    
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    setUser({ username: user, email: userEmail })
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
    delete axios.defaults.headers.common['Authorization']
    setUser(null)
  }

  if (loading) {
    return <div className="flex items-center justify-center min-h-screen">加载中...</div>
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
