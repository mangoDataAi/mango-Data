-- 添加父级域ID字段
ALTER TABLE t_domain ADD parent_id VARCHAR(32) DEFAULT NULL;
COMMENT ON COLUMN t_domain.parent_id IS '父级域ID';

-- 添加外键约束
ALTER TABLE t_domain ADD CONSTRAINT fk_domain_parent 
    FOREIGN KEY (parent_id) REFERENCES t_domain(id);

-- 添加索引
CREATE INDEX idx_domain_parent ON t_domain(parent_id); 