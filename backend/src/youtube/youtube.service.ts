import { Injectable, NotFoundException, ForbiddenException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';
import { AddYouTubeItemDto } from './dto/add-youtube-item.dto';

@Injectable()
export class YouTubeService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eventsGateway: EventsGateway,
  ) {}

  /**
   * Pre-curated educational, high-quality, verified safe channels for parents to choose from.
   */
  getCuratedEducationalChannels() {
    return [
      {
        channelId: 'UCXVCgDuD_XCkHYn-v95i75Q',
        title: 'Crash Course Kids',
        category: 'SCIENCE',
        description: 'Earth science, physical science, biology, and engineering for kids.',
        thumbnailUrl: 'https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=200',
      },
      {
        channelId: 'UCkJEpny5wQ5h2b_5GZ9X_3g',
        title: 'SciShow Kids',
        category: 'SCIENCE',
        description: 'Explores fun and curious science questions with Jessi and Squeaks.',
        thumbnailUrl: 'https://images.unsplash.com/photo-1507668077129-56e32842fceb?w=200',
      },
      {
        channelId: 'UCsooa4yRKGN_zEE8iknghZA',
        title: 'TED-Ed',
        category: 'EDUCATION',
        description: 'Carefully curated educational animations sparking curiosity.',
        thumbnailUrl: 'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=200',
      },
      {
        channelId: 'UCv6DY34g7Z03wLq_U3q3h-w',
        title: 'National Geographic Kids',
        category: 'NATURE',
        description: 'Amazing animals, exciting science, and fun nature explorations.',
        thumbnailUrl: 'https://images.unsplash.com/photo-1474511320723-9a56873867b5?w=200',
      },
      {
        channelId: 'UC8B_SfZ6XbU49eF91eU_4kA',
        title: 'NASA Space Place',
        category: 'SPACE',
        description: 'Interactive space exploration, planetary discovery, and astrophysics for children.',
        thumbnailUrl: 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200',
      },
    ];
  }

  /**
   * Parent retrieves all approved channels and videos for child profile.
   */
  async getApprovedLibrary(parentId: string, childId: string) {
    await this.verifyParentChild(parentId, childId);

    const items = await this.prisma.youTubeRestriction.findMany({
      where: {
        childId,
        isWhitelisted: true,
      },
      orderBy: { createdAt: 'desc' },
    });

    const webConfig = await this.prisma.webSafetyConfig.findUnique({
      where: { childId },
    });

    return {
      childId,
      youtubeMode: webConfig?.youtubeMode || 'SAFETUBE',
      dailyLimitMinutes: webConfig?.dailyYouTubeLimitMinutes || 60,
      approvedItems: items,
      curatedSuggestions: this.getCuratedEducationalChannels(),
    };
  }

  /**
   * Parent adds a channel or video to the approved whitelist library.
   */
  async addApprovedItem(parentId: string, childId: string, dto: AddYouTubeItemDto) {
    await this.verifyParentChild(parentId, childId);

    const existing = await this.prisma.youTubeRestriction.findUnique({
      where: {
        childId_externalId: {
          childId,
          externalId: dto.externalId.trim(),
        },
      },
    });

    if (existing && existing.isWhitelisted) {
      throw new ConflictException('This channel or video is already in the approved library.');
    }

    const item = await this.prisma.youTubeRestriction.upsert({
      where: {
        childId_externalId: {
          childId,
          externalId: dto.externalId.trim(),
        },
      },
      update: {
        type: dto.type,
        title: dto.title.trim(),
        description: dto.description?.trim(),
        thumbnailUrl: dto.thumbnailUrl,
        isWhitelisted: true,
      },
      create: {
        childId,
        type: dto.type,
        externalId: dto.externalId.trim(),
        title: dto.title.trim(),
        description: dto.description?.trim(),
        thumbnailUrl: dto.thumbnailUrl,
        isWhitelisted: true,
      },
    });

    // Real-Time Push to child device
    this.eventsGateway.broadcastPolicyUpdate(childId, {
      youtubeLibraryUpdated: true,
      addedItem: item,
    });

    return {
      message: `"${dto.title}" added to parent-approved YouTube library.`,
      item,
    };
  }

  /**
   * Parent removes an item from the whitelist library.
   */
  async removeApprovedItem(parentId: string, childId: string, externalId: string) {
    await this.verifyParentChild(parentId, childId);

    await this.prisma.youTubeRestriction.deleteMany({
      where: {
        childId,
        externalId,
      },
    });

    this.eventsGateway.broadcastPolicyUpdate(childId, {
      youtubeLibraryUpdated: true,
      removedExternalId: externalId,
    });

    return { message: 'Item removed from approved library.' };
  }

  /**
   * Child device SafeTube Player sync endpoint.
   * Returns ONLY approved items so the player cannot play unapproved content.
   */
  async getSafeTubeFeed(childId: string) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
      include: {
        webSafetyConfig: true,
        youtubeRestrictions: {
          where: { isWhitelisted: true },
        },
      },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    return {
      childId: child.id,
      childName: child.name,
      youtubeMode: child.webSafetyConfig?.youtubeMode || 'SAFETUBE',
      dailyLimitMinutes: child.webSafetyConfig?.dailyYouTubeLimitMinutes || 60,
      approvedItems: child.youtubeRestrictions,
    };
  }

  private async verifyParentChild(parentId: string, childId: string) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    if (child.parentId !== parentId) {
      throw new ForbiddenException('Unauthorized.');
    }

    return child;
  }
}
