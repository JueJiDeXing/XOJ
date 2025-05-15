package com.jjdx.xoj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjdx.xoj.mapper.FileRecordMapper;
import com.jjdx.xoj.model.entity.FileRecord;
import com.jjdx.xoj.service.FileRecordService;
import org.springframework.stereotype.Service;

/**
 * 文件记录服务实现
 */
@Service
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord> implements FileRecordService {
}
