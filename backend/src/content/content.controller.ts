import { Controller, Get, Post, Put, Delete, Body, Param, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { ContentService } from './content.service';
import { UpdateWebSafetyDto } from './dto/update-web-safety.dto';
import { AddWebsiteRuleDto } from './dto/website-rule.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('Web & Browser Filtering')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller('api/v1/content')
export class ContentController {
  constructor(private readonly contentService: ContentService) {}

  @Get(':childId/web-safety')
  @ApiOperation({ summary: 'Get web safety config, custom domain rules, and Managed Chrome bundle' })
  async getWebSafety(@Request() req, @Param('childId') childId: string) {
    return this.contentService.getWebSafety(req.user.parentId, childId);
  }

  @Put(':childId/web-safety')
  @ApiOperation({ summary: 'Update web safety toggles (SafeSearch, Adult filter, YouTube mode)' })
  async updateWebSafety(
    @Request() req,
    @Param('childId') childId: string,
    @Body() dto: UpdateWebSafetyDto,
  ) {
    return this.contentService.updateWebSafety(req.user.parentId, childId, dto);
  }

  @Post(':childId/website-rules')
  @ApiOperation({ summary: 'Add a custom domain block or whitelist entry' })
  async addWebsiteRule(
    @Request() req,
    @Param('childId') childId: string,
    @Body() dto: AddWebsiteRuleDto,
  ) {
    return this.contentService.addWebsiteRule(req.user.parentId, childId, dto);
  }

  @Delete(':childId/website-rules/:ruleId')
  @ApiOperation({ summary: 'Remove a custom domain rule' })
  async deleteWebsiteRule(
    @Request() req,
    @Param('childId') childId: string,
    @Param('ruleId') ruleId: string,
  ) {
    return this.contentService.deleteWebsiteRule(req.user.parentId, childId, ruleId);
  }
}
