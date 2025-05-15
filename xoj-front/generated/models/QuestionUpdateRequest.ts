/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { JudgeCase } from './JudgeCase';
import type { JudgeConfig } from './JudgeConfig';
export type QuestionUpdateRequest = {
    answer?: string;
    content?: string;
    id?: number;
    isDelete?: number;
    judgeCaseList?: Array<JudgeCase>;
    judgeConfig?: JudgeConfig;
    tagList?: Array<string>;
    title?: string;
};

