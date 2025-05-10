import { Graph, Node, Edge } from '@antv/x6'

// 注册自定义节点
Graph.registerNode('dependency-node', {
  inherit: 'rect',
  width: 180,
  height: 60,
  attrs: {
    body: {
      fill: '#ffffff',
      stroke: '#d9d9d9',
      strokeWidth: 1,
      rx: 4,
      ry: 4,
      shadowColor: '#00000026',
      shadowBlur: 5,
      shadowOffsetX: 0,
      shadowOffsetY: 2
    },
    label: {
      fontSize: 12,
      fill: '#595959',
      refX: 0.5,
      refY: 0.5,
      textAnchor: 'middle',
      textVerticalAnchor: 'middle'
    }
  },
  markup: [
    {
      tagName: 'rect',
      selector: 'body'
    },
    {
      tagName: 'text',
      selector: 'label'
    }
  ]
}, true)

// 注册自定义边
Graph.registerEdge('dependency-edge', {
  inherit: 'edge',
  attrs: {
    line: {
      stroke: '#b1b1b1',
      strokeWidth: 1,
      targetMarker: {
        name: 'block',
        width: 8,
        height: 6
      }
    }
  },
  router: {
    name: 'er',
    args: {
      direction: 'V'
    }
  },
  connector: {
    name: 'rounded'
  }
}, true)

// 节点渲染函数
export const renderNode = (node) => {
  const data = node.getData()
  const status = data.status || 'pending'
  const statusColors = {
    running: '#1890ff',
    success: '#52c41a',
    error: '#ff4d4f',
    pending: '#d9d9d9'
  }

  node.attr({
    body: {
      stroke: statusColors[status]
    },
    label: {
      text: [data.name, data.type].filter(Boolean).join('\n')
    }
  })
}

// 边渲染函数
export const renderEdge = (edge) => {
  const data = edge.getData()
  const type = data?.type || 'default'
  const edgeStyles = {
    strong: {
      strokeWidth: 2,
      strokeDasharray: ''
    },
    weak: {
      strokeWidth: 1,
      strokeDasharray: '5 5'
    },
    default: {
      strokeWidth: 1,
      strokeDasharray: ''
    }
  }

  const style = edgeStyles[type] || edgeStyles.default
  edge.attr('line', style)
}

// 注册节点和边的渲染器
Graph.registerNodeView('dependency-node', {
  render() {
    renderNode(this.cell)
    return this
  }
})

Graph.registerEdgeView('dependency-edge', {
  render() {
    renderEdge(this.cell)
    return this
  }
}) 