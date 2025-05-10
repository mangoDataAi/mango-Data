/**
 * 格式化日期时间
 * @param {Date|string|number} date 日期对象、日期字符串或时间戳
 * @param {string} format 格式化模式，默认为 'YYYY-MM-DD HH:mm:ss'
 * @returns {string} 格式化后的日期字符串
 */
export function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return '';
  
  const d = typeof date === 'object' ? date : new Date(date);
  
  if (isNaN(d.getTime())) {
    return '';
  }
  
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
}

/**
 * 格式化文件大小
 * @param {number} bytes 字节数
 * @param {number} decimals 小数位数
 * @returns {string} 格式化后的文件大小
 */
export function formatFileSize(bytes, decimals = 2) {
  if (bytes === 0) return '0 B';
  
  const k = 1024;
  const dm = decimals < 0 ? 0 : decimals;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
}

/**
 * 格式化数字（增加千位分隔符）
 * @param {number} num 要格式化的数字
 * @param {number} decimals 小数位数
 * @returns {string} 格式化后的数字
 */
export function formatNumber(num, decimals = 2) {
  if (num === null || num === undefined) return '';
  
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  }).format(num);
}

/**
 * 格式化百分比
 * @param {number} num 要格式化的数字
 * @param {number} decimals 小数位数
 * @returns {string} 格式化后的百分比
 */
export function formatPercent(num, decimals = 2) {
  if (num === null || num === undefined) return '';
  
  return `${(num * 100).toFixed(decimals)}%`;
}

/**
 * 格式化金额
 * @param {number} amount 金额
 * @param {string} currency 货币符号，默认为人民币￥
 * @param {number} decimals 小数位数
 * @returns {string} 格式化后的金额
 */
export function formatCurrency(amount, currency = '￥', decimals = 2) {
  if (amount === null || amount === undefined) return '';
  
  return `${currency}${new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  }).format(amount)}`;
}

/**
 * 格式化时长（秒转为时分秒）
 * @param {number} seconds 秒数
 * @returns {string} 格式化后的时长
 */
export function formatDuration(seconds) {
  if (!seconds) return '0秒';
  
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = Math.floor(seconds % 60);
  
  let result = '';
  
  if (hours > 0) {
    result += `${hours}小时`;
  }
  
  if (minutes > 0) {
    result += `${minutes}分`;
  }
  
  if (remainingSeconds > 0 || result === '') {
    result += `${remainingSeconds}秒`;
  }
  
  return result;
}

export default {
  formatDate,
  formatFileSize,
  formatNumber,
  formatPercent,
  formatCurrency,
  formatDuration
}; 