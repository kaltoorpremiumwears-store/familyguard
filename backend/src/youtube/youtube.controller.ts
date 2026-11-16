import { Controller, Get, Post, Delete, Body, Param, UseGuards, Request } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth, ApiResponse } from '@nestjs/swagger';
import { YouTubeService } from './youtube.service';
import { AddYouTubeItemDto } from './dto/add-youtube-item.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@ApiTags('YouTube Content Governance')
@Controller('api/v1/youtube')
export class YouTubeController {
  constructor(private readonly youtubeService: YouTubeService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get(':childId/approved-library')
  @ApiOperation({ summary: 'Parent retrieves approved channels/videos and curated educational list' })
  async getApprovedLibrary(@Request() req, @Param('childId') childId: string) {
    return this.youtubeService.getApprovedLibrary(req.user.parentId, childId);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post(':childId/approved-library')
  @ApiOperation({ summary: 'Parent adds a channel or video to approved library' })
  async addApprovedItem(
    @Request() req,
    @Param('childId') childId: string,
    @Body() dto: AddYouTubeItemDto,
  ) {
    return this.youtubeService.addApprovedItem(req.user.parentId, childId, dto);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Delete(':childId/approved-library/:externalId')
  @ApiOperation({ summary: 'Parent removes an item from approved library' })
  async removeApprovedItem(
    @Request() req,
    @Param('childId') childId: string,
    @Param('externalId') externalId: string,
  ) {
    return this.youtubeService.removeApprovedItem(req.user.parentId, childId, externalId);
  }

  @Get('safetube-feed/:childId')
  @ApiOperation({ summary: 'Child SafeTube player endpoint: delivers strictly whitelisted video feed' })
  async getSafeTubeFeed(@Param('childId') childId: string) {
    return this.youtubeService.getSafeTubeFeed(childId);
  }
}
