import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/modules/user'

const service = axios.create({
    baseURL: '/',
    timeout: 300000,
    headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
    }
})

// 请求拦截器
service.interceptors.request.use(
    config => {
        console.log('Request:', {
            url: config.url,
            method: config.method,
            data: config.data,
            params: config.params
        })
        const userStore = useUserStore()
        const token = userStore.token
        if (token) {
            // 将token添加到header中
            config.headers['Authorization'] = `Bearer ${token}`
        } else {
            // 如果没有token，从localStorage中获取
            const localToken = localStorage.getItem('token')
            if (localToken) {
                config.headers['Authorization'] = `Bearer ${localToken}`
            }
        }
        console.log('发送请求:', config.url, config.params || config.data)
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    response => {
        console.log('原始响应:', response)
        const res = response.data
        console.log('响应数据:', res)
        
        // 如果响应成功但没有数据，返回原始响应
        if (!res) {
            console.log('响应没有数据，返回原始响应')
            return response
        }

        // 检查是否为R格式响应 (code字段)
        if (res.code !== undefined) {
            console.log('检测到R格式响应, code =', res.code)
            
            // 如果响应码为0，说明接口调用成功
            if (res.code === 0) {
                console.log('接口调用成功, 返回数据')
                // 添加success属性以匹配前端期望的格式
                return {
                    ...res,
                    success: true,
                    data: res.data
                }
            }
            
            // 如果响应码不为0，说明接口出错
            const errorMsg = res.msg || res.message || '系统错误'
            console.error('接口调用失败:', errorMsg)
            
            ElMessage({
                message: errorMsg,
                type: 'error',
                duration: 5 * 1000
            })
            
            // 特定错误码处理
            if (res.code === 401) {
                console.log('检测到401错误，执行登出操作')
                try {
                    const userStore = useUserStore()
                    userStore.logout()
                    router.push('/login')
                } catch (e) {
                    console.error('登出操作失败:', e)
                }
            }
            
            return Promise.reject(new Error(errorMsg))
        }
        
        // 不是标准R格式，直接返回
        console.log('非标准R格式响应，直接返回')
        console.log('收到响应:', response.config.url, response.data)
        return res
    },
    error => {
        console.error('响应错误:', error)
        if (error.response) {
            console.error('错误响应数据:', error.response.data)
            console.error('错误状态码:', error.response.status)
        }
        const message = error.response?.data?.message || error.message || '请求失败'
        ElMessage({
            message: message,
            type: 'error',
            duration: 5 * 1000
        })
        return Promise.reject(error)
    }
)

export default service
